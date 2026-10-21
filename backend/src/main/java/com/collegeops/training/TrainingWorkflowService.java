package com.collegeops.training;

import com.collegeops.common.AuditEvent;
import com.collegeops.common.AuditEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class TrainingWorkflowService {
    private final JdbcTemplate jdbc;
    private final AuditEventRepository audit;

    public TrainingWorkflowService(JdbcTemplate jdbc, AuditEventRepository audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public List<Map<String,Object>> jobRoles(UUID tenant) {
        return rows("select id,name,description,created_at from training_job_roles where institution_id=? order by name", tenant);
    }

    @Transactional
    public Map<String,Object> createRole(UUID tenant, UUID actor, String name, String description) {
        if (jdbc.queryForObject("select count(*) from training_job_roles where institution_id=? and lower(name)=lower(?)",Integer.class,tenant,name.strip())>0)
            throw new TrainingRequestException(409,"CONFLICT","A job role with this name already exists.");
        UUID id = UUID.randomUUID();
        jdbc.update("insert into training_job_roles(id,institution_id,name,description,created_at,updated_at) values(?,?,?,?,?,?)", id, tenant, name.strip(), trim(description), Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        record(tenant, actor, "TRAINING_JOB_ROLE_CREATED", "TRAINING_JOB_ROLE", id, name.strip());
        return normalized(jdbc.queryForMap("select id,name,description,created_at from training_job_roles where id=? and institution_id=?", id, tenant));
    }

    @Transactional
    public void requireSkill(UUID tenant, UUID actor, UUID roleId, UUID skillId, int targetLevel) {
        requireExists("select count(*) from training_job_roles where id=? and institution_id=?", roleId, tenant);
        requireExists("select count(*) from training_skills where id=? and institution_id=?", skillId, tenant);
        if(jdbc.queryForObject("select count(*) from training_role_skill_requirements where job_role_id=? and skill_id=?",Integer.class,roleId,skillId)>0)
            throw new TrainingRequestException(409,"CONFLICT","This skill requirement already exists for the role.");
        UUID id = UUID.randomUUID();
        jdbc.update("insert into training_role_skill_requirements(id,institution_id,job_role_id,skill_id,target_level) values(?,?,?,?,?)", id, tenant, roleId, skillId, targetLevel);
        record(tenant, actor, "TRAINING_ROLE_SKILL_REQUIRED", "TRAINING_JOB_ROLE", roleId, "skill=" + skillId + "; target=" + targetLevel);
    }

    public List<Map<String,Object>> roleSkills(UUID tenant, UUID role) {
        requireExists("select count(*) from training_job_roles where id=? and institution_id=?", role, tenant);
        return rows("select s.id,s.name,r.target_level from training_role_skill_requirements r join training_skills s on s.id=r.skill_id and s.institution_id=r.institution_id where r.job_role_id=? and r.institution_id=? order by s.name", role, tenant);
    }

    public List<Map<String,Object>> pathways(UUID tenant) {
        return rows("select id,name,version,description,status,synthetic,created_at from training_pathways where institution_id=? order by name,version desc", tenant);
    }

    @Transactional
    public Map<String,Object> createPathway(UUID tenant, UUID actor, String name, String description) {
        int version = jdbc.queryForObject("select coalesce(max(version),0)+1 from training_pathways where institution_id=? and name=?", Integer.class, tenant, name.strip());
        UUID id = UUID.randomUUID(); Instant now = Instant.now();
        jdbc.update("insert into training_pathways(id,institution_id,name,version,description,status,synthetic,created_at,updated_at) values(?,?,?,?,?,'DRAFT',false,?,?)", id, tenant, name.strip(), version, trim(description), Timestamp.from(now), Timestamp.from(now));
        record(tenant, actor, "TRAINING_PATHWAY_CREATED", "TRAINING_PATHWAY", id, name.strip()+" v"+version);
        return normalized(jdbc.queryForMap("select id,name,version,description,status,synthetic,created_at from training_pathways where id=?", id));
    }

    @Transactional
    public Map<String,Object> addModule(UUID tenant, UUID actor, UUID pathway, int sequence, String title, String description, int hours) {
        Map<String,Object> row = one("select status from training_pathways where id=? and institution_id=?", pathway, tenant);
        if (!"DRAFT".equals(row.get("status"))) bad("Modules can only be changed while a pathway is a draft.");
        UUID id = UUID.randomUUID();
        int order = sequence == 0 ? jdbc.queryForObject("select coalesce(max(sequence_number),0)+1 from training_pathway_modules where pathway_id=? and institution_id=?",Integer.class,pathway,tenant) : sequence;
        jdbc.update("insert into training_pathway_modules(id,institution_id,pathway_id,sequence_number,title,description,estimated_hours) values(?,?,?,?,?,?,?)", id, tenant, pathway, order, title.strip(), trim(description), hours);
        record(tenant, actor, "TRAINING_MODULE_CREATED", "TRAINING_PATHWAY", pathway, title.strip());
        return normalized(jdbc.queryForMap("select id,sequence_number,title,description,estimated_hours from training_pathway_modules where id=?", id));
    }

    @Transactional
    public void publishPathway(UUID tenant, UUID actor, UUID pathway) {
        Map<String,Object> row = one("select status from training_pathways where id=? and institution_id=?", pathway, tenant);
        if (!"DRAFT".equals(row.get("status"))) bad("Only a draft pathway can be published.");
        if (jdbc.queryForObject("select count(*) from training_pathway_modules where pathway_id=? and institution_id=?", Integer.class, pathway, tenant) == 0) bad("A pathway needs at least one module before publishing.");
        jdbc.update("update training_pathways set status='PUBLISHED',updated_at=? where id=? and institution_id=?", Timestamp.from(Instant.now()), pathway, tenant);
        record(tenant, actor, "TRAINING_PATHWAY_PUBLISHED", "TRAINING_PATHWAY", pathway, "Published pathway version");
    }

    public List<Map<String,Object>> modules(UUID tenant, UUID pathway) {
        requireExists("select count(*) from training_pathways where id=? and institution_id=?", pathway, tenant);
        return rows("select id,sequence_number,title,description,estimated_hours from training_pathway_modules where pathway_id=? and institution_id=? order by sequence_number", pathway, tenant);
    }

    @Transactional
    public Map<String,Object> createCohort(UUID tenant, UUID actor, UUID trainer, UUID pathway, String name, LocalDate start, LocalDate end) {
        if(end!=null && end.isBefore(start)) throw new TrainingRequestException(400,"VALIDATION_ERROR","Cohort end date cannot be before its start date.");
        requireExists("select count(*) from training_pathways where id=? and institution_id=? and status='PUBLISHED'", pathway, tenant);
        requireExists("select count(*) from users u join user_roles r on r.user_id=u.id where u.id=? and u.institution_id=? and u.status='ACTIVE' and r.role='TRAINER'", trainer, tenant);
        UUID id=UUID.randomUUID(); Instant now=Instant.now();
        jdbc.update("insert into training_cohorts(id,institution_id,pathway_id,name,status,start_date,end_date,trainer_user_id,created_at,updated_at) values(?,?,?,?,'PLANNED',?,?,?,?,?)", id,tenant,pathway,name.strip(),start,end,trainer,Timestamp.from(now),Timestamp.from(now));
        record(tenant,actor,"TRAINING_COHORT_CREATED","TRAINING_COHORT",id,name.strip());
        return normalized(jdbc.queryForMap("select id,pathway_id,name,status,start_date,end_date,trainer_user_id from training_cohorts where id=?",id));
    }

    @Transactional
    public void activateCohort(UUID tenant, UUID actor, UUID cohort) {
        Map<String,Object> row=one("select status from training_cohorts where id=? and institution_id=?",cohort,tenant);
        if (!"PLANNED".equals(row.get("status"))) bad("Only a planned cohort can be activated.");
        jdbc.update("update training_cohorts set status='ACTIVE',updated_at=? where id=? and institution_id=?",Timestamp.from(Instant.now()),cohort,tenant);
        record(tenant,actor,"TRAINING_COHORT_ACTIVATED","TRAINING_COHORT",cohort,"Activated cohort");
    }

    public List<Map<String,Object>> cohorts(UUID tenant, UUID actor, boolean student) {
        if (student) return rows("select c.id,c.name,c.status,c.start_date,c.end_date from training_cohorts c join training_enrollments e on e.cohort_id=c.id and e.institution_id=c.institution_id join students s on s.id=e.student_id where c.institution_id=? and s.user_id=? order by c.start_date desc",tenant,actor);
        return rows("select id,name,status,start_date,end_date,pathway_id,trainer_user_id from training_cohorts where institution_id=? order by start_date desc",tenant);
    }

    @Transactional
    public void enroll(UUID tenant, UUID actor, UUID cohort, UUID studentId) {
        Map<String,Object> c=one("select status from training_cohorts where id=? and institution_id=?",cohort,tenant);
        if (!"PLANNED".equals(c.get("status")) && !"ACTIVE".equals(c.get("status"))) bad("Students can only enroll in planned or active cohorts.");
        requireExists("select count(*) from students where id=? and institution_id=? and status='ACTIVE'",studentId,tenant);
        if(jdbc.queryForObject("select count(*) from training_enrollments where cohort_id=? and student_id=?",Integer.class,cohort,studentId)>0)
            throw new TrainingRequestException(409,"CONFLICT","This student is already enrolled in the cohort.");
        UUID id=UUID.randomUUID();
        jdbc.update("insert into training_enrollments(id,institution_id,cohort_id,student_id,status,enrolled_at) values(?,?,?,?,'ENROLLED',?)",id,tenant,cohort,studentId,Timestamp.from(Instant.now()));
        record(tenant,actor,"TRAINING_STUDENT_ENROLLED","TRAINING_ENROLLMENT",id,"cohort="+cohort);
    }

    @Transactional
    public Map<String,Object> createAssignment(UUID tenant, UUID actor, UUID cohort, UUID module, String title, String instructions, Instant due, List<RubricItem> rubric) {
        Map<String,Object> c=one("select status,pathway_id from training_cohorts where id=? and institution_id=?",cohort,tenant);
        if (!"ACTIVE".equals(c.get("status"))) bad("Assignments can only be created for active cohorts.");
        if (module != null) requireExists("select count(*) from training_pathway_modules where id=? and institution_id=? and pathway_id=?",module,tenant,c.get("pathwayId"));
        UUID id=UUID.randomUUID();
        jdbc.update("insert into training_assignments(id,institution_id,cohort_id,module_id,title,instructions,due_at,created_by,created_at) values(?,?,?,?,?,?,?,?,?)",id,tenant,cohort,module,title.strip(),instructions.strip(),due==null?null:Timestamp.from(due),actor,Timestamp.from(Instant.now()));
        int n=1;
        for (RubricItem item: rubric) jdbc.update("insert into training_assignment_rubric_items(id,institution_id,assignment_id,description,max_points,sequence_number) values(?,?,?,?,?,?)",UUID.randomUUID(),tenant,id,item.description().strip(),item.maxPoints(),n++);
        record(tenant,actor,"TRAINING_ASSIGNMENT_CREATED","TRAINING_ASSIGNMENT",id,title.strip());
        return normalized(jdbc.queryForMap("select id,cohort_id,module_id,title,instructions,due_at,created_at from training_assignments where id=?",id));
    }

    public List<Map<String,Object>> assignments(UUID tenant, UUID actor, UUID cohort, boolean student) {
        requireExists("select count(*) from training_cohorts where id=? and institution_id=?",cohort,tenant);
        if(student) return rows("select a.id,a.cohort_id,a.module_id,a.title,a.instructions,a.due_at from training_assignments a join training_enrollments e on e.cohort_id=a.cohort_id and e.institution_id=a.institution_id join students s on s.id=e.student_id where a.institution_id=? and a.cohort_id=? and s.user_id=? and e.status='ENROLLED' order by a.due_at",tenant,cohort,actor);
        return rows("select id,cohort_id,module_id,title,instructions,due_at from training_assignments where institution_id=? and cohort_id=? order by due_at",tenant,cohort);
    }

    public List<Map<String,Object>> rubric(UUID tenant, UUID assignment) {
        requireExists("select count(*) from training_assignments where id=? and institution_id=?",assignment,tenant);
        return rows("select id,description,max_points,sequence_number from training_assignment_rubric_items where assignment_id=? and institution_id=? order by sequence_number",assignment,tenant);
    }

    @Transactional
    public void submit(UUID tenant, UUID actor, UUID assignment, UUID student, String text) {
        Map<String,Object> a=one("select cohort_id,due_at from training_assignments where id=? and institution_id=?",assignment,tenant);
        requireExists("select count(*) from training_enrollments where cohort_id=? and student_id=? and institution_id=? and status='ENROLLED'",a.get("cohortId"),student,tenant);
        if (a.get("dueAt")!=null && asInstant(a.get("dueAt")).isBefore(Instant.now())) bad("The assignment due date has passed.");
        int attempt=jdbc.queryForObject("select coalesce(max(attempt_number),0)+1 from training_assessment_attempts where assignment_id=? and student_id=?",Integer.class,assignment,student);
        UUID id=UUID.randomUUID();
        jdbc.update("insert into training_assessment_attempts(id,institution_id,assignment_id,student_id,attempt_number,status,submission_text,submitted_at) values(?,?,?,?,?,'SUBMITTED',?,?)",id,tenant,assignment,student,attempt,text.strip(),Timestamp.from(Instant.now()));
        record(tenant,actor,"TRAINING_ASSESSMENT_SUBMITTED","TRAINING_ASSESSMENT_ATTEMPT",id,"assignment="+assignment);
    }

    @Transactional
    public void grade(UUID tenant, UUID actor, UUID attempt, String feedback, List<RubricScore> scores) {
        Map<String,Object> row=one("select assignment_id,status from training_assessment_attempts where id=? and institution_id=?",attempt,tenant);
        if (!"SUBMITTED".equals(row.get("status"))) bad("Only submitted attempts can be graded once.");
        List<Map<String,Object>> rubric=rows("select id,max_points from training_assignment_rubric_items where assignment_id=? and institution_id=?",row.get("assignmentId"),tenant);
        if (rubric.isEmpty() || scores.size()!=rubric.size()) bad("A score is required for every rubric item.");
        Map<UUID,Integer> max=new HashMap<>(); rubric.forEach(r->max.put((UUID)r.get("id"),(Integer)r.get("maxPoints")));
        int total=0;
        Set<UUID> scored=new HashSet<>();
        for(RubricScore score:scores) { Integer cap=max.get(score.rubricItemId()); if(cap==null||score.pointsAwarded()>cap||!scored.add(score.rubricItemId())) bad("Rubric scores must match each assignment rubric item once and stay within its maximum."); total+=score.pointsAwarded(); }
        if(scored.size()!=max.size()) bad("A score is required for every rubric item.");
        for(RubricScore score:scores) jdbc.update("insert into training_attempt_rubric_scores(id,institution_id,attempt_id,rubric_item_id,points_awarded) values(?,?,?,?,?)",UUID.randomUUID(),tenant,attempt,score.rubricItemId(),score.pointsAwarded());
        jdbc.update("update training_assessment_attempts set status='GRADED',score=?,feedback=?,graded_by=?,graded_at=? where id=? and institution_id=?",total,feedback.strip(),actor,Timestamp.from(Instant.now()),attempt,tenant);
        record(tenant,actor,"TRAINING_ASSESSMENT_GRADED","TRAINING_ASSESSMENT_ATTEMPT",attempt,"score="+total);
    }

    @Transactional
    public void feedback(UUID tenant, UUID actor, UUID cohort, UUID student, String message) {
        requireExists("select count(*) from training_cohorts where id=? and institution_id=? and status in ('PLANNED','ACTIVE')",cohort,tenant);
        requireExists("select count(*) from training_enrollments where cohort_id=? and student_id=? and institution_id=? and status='ENROLLED'",cohort,student,tenant);
        UUID id=UUID.randomUUID();
        jdbc.update("insert into training_feedback(id,institution_id,cohort_id,student_id,trainer_user_id,message,created_at) values(?,?,?,?,?,?,?)",id,tenant,cohort,student,actor,message.strip(),Timestamp.from(Instant.now()));
        record(tenant,actor,"TRAINING_FEEDBACK_ADDED","TRAINING_FEEDBACK",id,"cohort="+cohort);
    }

    public List<Map<String,Object>> feedback(UUID tenant, UUID actor, UUID cohort, boolean student) {
        requireExists("select count(*) from training_cohorts where id=? and institution_id=?",cohort,tenant);
        if(student) return rows("select f.id,f.cohort_id,f.message,f.created_at from training_feedback f join students s on s.id=f.student_id where f.institution_id=? and f.cohort_id=? and s.user_id=? order by f.created_at desc",tenant,cohort,actor);
        return rows("select id,cohort_id,student_id,message,trainer_user_id,created_at from training_feedback where institution_id=? and cohort_id=? order by created_at desc",tenant,cohort);
    }

    public List<Map<String,Object>> results(UUID tenant, UUID actor, boolean student) {
        if(student) return rows("select a.id,a.assignment_id,a.attempt_number,a.status,a.score,a.feedback,a.submitted_at,a.graded_at from training_assessment_attempts a join students s on s.id=a.student_id where a.institution_id=? and s.user_id=? order by a.submitted_at desc",tenant,actor);
        return rows("select id,assignment_id,student_id,attempt_number,status,score,feedback,submitted_at,graded_at from training_assessment_attempts where institution_id=? order by submitted_at desc",tenant);
    }

    public UUID studentId(UUID tenant, UUID actor) {
        try { return jdbc.queryForObject("select id from students where institution_id=? and user_id=? and status='ACTIVE'",UUID.class,tenant,actor); }
        catch(org.springframework.dao.EmptyResultDataAccessException e) { throw new TrainingRequestException(404,"NOT_FOUND","Student profile was not found in this institution."); }
    }

    @Transactional
    public void seedJavaPathway(UUID tenant, UUID actor) {
        Integer count=jdbc.queryForObject("select count(*) from training_pathways where institution_id=? and name='Synthetic Java and Spring Boot Foundations'",Integer.class,tenant);
        if(count>0) return;
        UUID path=UUID.randomUUID(); Instant now=Instant.now();
        jdbc.update("insert into training_pathways(id,institution_id,name,version,description,status,synthetic,created_at,updated_at) values(?,?,?,1,?,'PUBLISHED',true,?,?)",path,tenant,"Synthetic Java and Spring Boot Foundations","SYNTHETIC DEMONSTRATION DATA. Internally defined learning pathway; not employer validated and makes no hiring or placement claims.",Timestamp.from(now),Timestamp.from(now));
        Object[][] modules={{"Java language fundamentals","Types, control flow, classes and object design",12},{"Collections, exceptions and clean code","Collections APIs, defensive coding and error handling",12},{"SQL and relational databases","Queries, constraints, joins and transaction basics",10},{"REST APIs","HTTP methods, status codes and request validation",10},{"Spring Boot foundations","Dependency injection, configuration and web endpoints",16},{"Persistence with Spring Data","JPA mapping, repositories and migrations",12},{"Security fundamentals","Authentication concepts, authorization and tenant boundaries",10},{"Testing and debugging","Unit tests, integration tests and debugging workflow",12},{"Git and basic CI","Branches, reviews, automated checks and build pipelines",8},{"Practical project","A small synthetic service with tests and documentation",20}};
        for(int i=0;i<modules.length;i++) jdbc.update("insert into training_pathway_modules(id,institution_id,pathway_id,sequence_number,title,description,estimated_hours) values(?,?,?,?,?,?,?)",UUID.randomUUID(),tenant,path,i+1,modules[i][0],modules[i][1],modules[i][2]);
        record(tenant,actor,"SYNTHETIC_TRAINING_PATHWAY_SEEDED","TRAINING_PATHWAY",path,"Synthetic Java and Spring Boot demonstration pathway v1");
    }

    public record RubricItem(String description, int maxPoints) {}
    public record RubricScore(UUID rubricItemId, int pointsAwarded) {}
    private void record(UUID tenant, UUID actor, String action, String type, UUID target, String summary) { audit.save(new AuditEvent(tenant,actor,action,type,target,summary)); }
    private void requireExists(String sql, Object... args) { if(jdbc.queryForObject(sql,Integer.class,args)==0) throw new TrainingRequestException(404,"NOT_FOUND","Training resource was not found in this institution."); }
    private List<Map<String,Object>> rows(String sql,Object... args) {
        return jdbc.queryForList(sql,args).stream().map(this::normalized).toList();
    }
    private Map<String,Object> normalized(Map<String,Object> source) {
        Map<String,Object> result=new LinkedHashMap<>();
        source.forEach((k,v)->{
            String[] parts=k.toLowerCase(Locale.ROOT).split("_");
            StringBuilder key=new StringBuilder(parts[0]);
            for(int i=1;i<parts.length;i++) key.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            result.put(key.toString(),v);
        });
        return result;
    }
    private Map<String,Object> one(String sql,Object... args) {
        try { return normalized(jdbc.queryForMap(sql,args)); }
        catch(org.springframework.dao.EmptyResultDataAccessException e) { throw new TrainingRequestException(404,"NOT_FOUND","Training resource was not found in this institution."); }
    }
    private void bad(String message) { throw new TrainingRequestException(409,"INVALID_TRANSITION",message); }
    private String trim(String s) { return s==null?null:s.strip(); }
    private Instant asInstant(Object value) {
        if(value instanceof Timestamp timestamp) return timestamp.toInstant();
        if(value instanceof java.time.OffsetDateTime dateTime) return dateTime.toInstant();
        if(value instanceof Instant instant) return instant;
        throw new IllegalStateException("Unsupported timestamp type from training assignment: " + value.getClass().getName());
    }
}
