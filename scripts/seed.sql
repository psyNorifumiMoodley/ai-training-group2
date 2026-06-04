-- ============================================================
-- seed.sql  —  Wipe all application data and re-seed for dev
-- Run: docker exec -i ai-training-group2-postgres-1 psql -U dap -d dap < scripts/seed.sql
-- Passwords: admin=admin123  marker=marker123  candidates=candidate123
-- ============================================================

-- 1. Truncate all application tables (CASCADE handles FK order)
TRUNCATE
  question_group_response_children,
  question_group_response,
  mcq_response,
  text_response,
  doc_response,
  response,
  feedback,
  submission,
  assessment_question_link,
  assessment,
  group_question_follow_up,
  group_question,
  text_question,
  mcq_question,
  doc_question,
  question_group_member,
  question_group,
  assessment_question,
  question_bank,
  candidate,
  app_user
CASCADE;

-- ============================================================
-- 2. Users  (admin=admin123  marker=marker123  3x candidate=candidate123)
-- ============================================================
INSERT INTO app_user (id, name, email, password_hash, role, created_at, updated_at) VALUES
  ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'Admin',          'admin@admin.com',       '$2b$10$RGphT8JJOa1sgl/W4GNzLOEl7XcHKy4S54GP9Ie8RQS77HhzjK2cW', 'ADMIN',     NOW(), NOW()),
  ('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'John Marker',    'marker@marker.com',     '$2b$10$RGphT8JJOa1sgl/W4GNzLO2IeKNjxoQbYqmhvKsKhCriNnoZoE3Ny', 'MARKER',    NOW(), NOW()),
  ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'Alice Developer', 'alice@candidate.com',   '$2b$10$RGphT8JJOa1sgl/W4GNzLOypoVzlAi.nUqQES1r.XZTt18JdJmfLS', 'CANDIDATE', NOW(), NOW()),
  ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 'Bob Engineer',    'bob@candidate.com',     '$2b$10$RGphT8JJOa1sgl/W4GNzLOypoVzlAi.nUqQES1r.XZTt18JdJmfLS', 'CANDIDATE', NOW(), NOW()),
  ('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', 'Charlie Coder',   'charlie@candidate.com', '$2b$10$RGphT8JJOa1sgl/W4GNzLOypoVzlAi.nUqQES1r.XZTt18JdJmfLS', 'CANDIDATE', NOW(), NOW());

INSERT INTO candidate (candidate_id, created_at, updated_at) VALUES
  ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', NOW(), NOW()),
  ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', NOW(), NOW()),
  ('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', NOW(), NOW());

-- ============================================================
-- 3. MCQ Questions (30)
-- ============================================================
INSERT INTO mcq_question (id, category, question, options, correct_answers, created_at, updated_at) VALUES

  (gen_random_uuid(), 'Java Basics',
   'Which keyword prevents a class from being subclassed in Java?',
   '["final","static","abstract","sealed"]'::jsonb,
   '["final"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Basics',
   'What is the default value of an int field declared in a Java class (not a local variable)?',
   '["0","null","-1","undefined"]'::jsonb,
   '["0"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Basics',
   'Which statement about the Java String class is correct?',
   '["String is mutable","String is final and cannot be extended","String can be safely subclassed","String is not thread-safe"]'::jsonb,
   '["String is final and cannot be extended"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Collections',
   'Which List implementation provides the best performance for frequent random access by index?',
   '["LinkedList","ArrayList","Stack","Vector"]'::jsonb,
   '["ArrayList"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Collections',
   'What does HashMap.get(key) return when the key does not exist in the map?',
   '["0","false","null","Throws NoSuchElementException"]'::jsonb,
   '["null"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java OOP',
   'Which access modifier restricts visibility to within the declaring class only?',
   '["public","protected","private","package-private"]'::jsonb,
   '["private"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java OOP',
   'What is method overloading in Java?',
   '["Redefining a method in a subclass","Multiple methods with the same name but different parameter lists","Calling a superclass method from a subclass","Hiding a parent class method with a static method"]'::jsonb,
   '["Multiple methods with the same name but different parameter lists"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Which class-level annotation implies @ResponseBody on every handler method in that class?',
   '["@Controller","@RestController","@Service","@Component"]'::jsonb,
   '["@RestController"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'What is the primary purpose of the @Transactional annotation in Spring?',
   '["Mark a method as a REST endpoint","Manage database transaction boundaries","Inject a bean dependency","Configure Spring Security rules"]'::jsonb,
   '["Manage database transaction boundaries"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Which Spring Data JPA method returns an empty Optional instead of throwing an exception when an entity is not found?',
   '["findById()","getOne()","getById()","getReferenceById()"]'::jsonb,
   '["findById()"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'SQL',
   'Which SQL clause filters rows after a GROUP BY aggregation?',
   '["WHERE","HAVING","GROUP BY","ORDER BY"]'::jsonb,
   '["HAVING"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'SQL',
   'Which JOIN type returns all rows from both tables, filling NULL for non-matching rows on either side?',
   '["INNER JOIN","LEFT JOIN","RIGHT JOIN","FULL OUTER JOIN"]'::jsonb,
   '["FULL OUTER JOIN"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'SQL',
   'What does the SQL COALESCE function return?',
   '["Concatenation of all non-null arguments","The first non-null value in the argument list","The count of non-null arguments","The maximum of all arguments"]'::jsonb,
   '["The first non-null value in the argument list"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'JPA/Hibernate',
   'Which FetchType loads associated entities immediately when the owning entity is loaded?',
   '["LAZY","EAGER","FETCH","IMMEDIATE"]'::jsonb,
   '["EAGER"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'JPA/Hibernate',
   'What does @Column(nullable = false) enforce at the database level?',
   '["A runtime null check in Java","A NOT NULL constraint on the database column","Both a Java null check and a DB constraint","It is documentation only and enforces nothing"]'::jsonb,
   '["A NOT NULL constraint on the database column"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Design Patterns',
   'Which design pattern ensures that only one instance of a class exists throughout the application?',
   '["Factory","Observer","Singleton","Strategy"]'::jsonb,
   '["Singleton"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Design Patterns',
   'Which pattern defines a one-to-many dependency so that when one object changes state all its dependents are notified automatically?',
   '["Strategy","Command","Observer","Decorator"]'::jsonb,
   '["Observer"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'Which HTTP status code indicates that a resource was successfully created?',
   '["200 OK","201 Created","204 No Content","400 Bad Request"]'::jsonb,
   '["201 Created"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'Which HTTP method is idempotent but NOT safe?',
   '["GET","PUT","POST","PATCH"]'::jsonb,
   '["PUT"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Algorithms',
   'What is the time complexity of binary search on a sorted array of n elements?',
   '["O(n)","O(log n)","O(n log n)","O(1)"]'::jsonb,
   '["O(log n)"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Algorithms',
   'Which sorting algorithm has the best average-case time complexity among the options below?',
   '["Bubble Sort","Selection Sort","Quick Sort","Insertion Sort"]'::jsonb,
   '["Quick Sort"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Data Structures',
   'Which data structure follows Last In, First Out (LIFO) ordering?',
   '["Queue","Stack","Heap","Deque"]'::jsonb,
   '["Stack"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Data Structures',
   'What is the average time complexity of inserting a key-value pair into a HashMap?',
   '["O(1)","O(log n)","O(n)","O(n squared)"]'::jsonb,
   '["O(1)"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Concurrency',
   'Which Java class provides atomic integer operations without requiring explicit synchronization blocks?',
   '["SynchronizedInteger","AtomicInteger","volatile int wrapper","ThreadLocalInteger"]'::jsonb,
   '["AtomicInteger"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Concurrency',
   'What does the volatile keyword guarantee in Java?',
   '["Atomicity of compound read-modify-write operations","Visibility of changes to the variable across all threads","Both atomicity and visibility","Neither — volatile is deprecated in modern Java"]'::jsonb,
   '["Visibility of changes to the variable across all threads"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Git',
   'Which git command creates a new branch and checks it out in a single step?',
   '["git branch new-branch","git checkout new-branch","git checkout -b new-branch","git branch --create new-branch"]'::jsonb,
   '["git checkout -b new-branch"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Testing',
   'What is the primary purpose of mocking in unit tests?',
   '["To make tests run slower and more realistic","To isolate the unit under test from its external dependencies","To replace all automated tests with manual stubs","To verify database integration"]'::jsonb,
   '["To isolate the unit under test from its external dependencies"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Maven',
   'Which Maven lifecycle phase compiles the source and runs unit tests?',
   '["compile","validate","test","package"]'::jsonb,
   '["test"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Docker',
   'What is the purpose of the EXPOSE instruction in a Dockerfile?',
   '["Opens a port on the host machine","Documents which port the container listens on at runtime","Creates a firewall rule on the container","Starts a service when the container launches"]'::jsonb,
   '["Documents which port the container listens on at runtime"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Security',
   'Which algorithm is recommended for hashing user passwords in a modern application?',
   '["MD5","SHA-1","SHA-256","BCrypt"]'::jsonb,
   '["BCrypt"]'::jsonb, NOW(), NOW());

-- ============================================================
-- 4. Text Questions (30 standalone)
-- ============================================================
INSERT INTO text_question (id, category, question, keywords, created_at, updated_at) VALUES

  (gen_random_uuid(), 'Java Basics',
   'Explain the difference between checked and unchecked exceptions in Java. When would you choose to use each?',
   '["checked","unchecked","RuntimeException","IOException","exception hierarchy"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Software Design',
   'What are the SOLID principles? Briefly explain each one and give an example of where you would apply it.',
   '["SOLID","Single Responsibility","Open/Closed","Liskov Substitution","Interface Segregation","Dependency Inversion"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Describe the difference between @Component, @Service, and @Repository annotations in Spring. Are they interchangeable?',
   '["@Component","@Service","@Repository","stereotype","exception translation"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'JPA/Hibernate',
   'What is the N+1 query problem in JPA? Describe a concrete scenario where it occurs and explain how to fix it.',
   '["N+1","lazy loading","JOIN FETCH","@EntityGraph","batch fetching"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Database',
   'Explain the difference between optimistic and pessimistic locking. When would you choose each approach?',
   '["optimistic locking","pessimistic locking","@Version","SELECT FOR UPDATE","concurrency"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Security',
   'Describe how Spring Security JWT authentication works. What happens from the moment a user submits credentials to receiving a token?',
   '["JWT","authentication filter","UserDetailsService","JwtProvider","token validation"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'What are the key benefits and drawbacks of microservices architecture compared to a monolith?',
   '["microservices","monolith","scalability","operational complexity","bounded context"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'Explain the six REST architectural constraints. Which do you consider most important and why?',
   '["stateless","uniform interface","cacheable","layered system","code on demand","client-server"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Basics',
   'Describe how garbage collection works in the JVM. What are the different heap generations and when is each collected?',
   '["garbage collection","young generation","old generation","GC roots","mark-and-sweep"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Concurrency',
   'Explain the difference between a thread and a process. How does Java''s threading model map to operating system threads?',
   '["thread","process","JVM","OS thread","context switching"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'SQL',
   'Explain the differences between INNER JOIN, LEFT JOIN, and FULL OUTER JOIN with a concrete example for each.',
   '["INNER JOIN","LEFT JOIN","FULL OUTER JOIN","NULL","result set"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Database',
   'What is a database transaction? Explain each of the ACID properties with a real-world example.',
   '["transaction","ACID","atomicity","consistency","isolation","durability"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Design Patterns',
   'Describe the Builder design pattern. Why would you prefer it over a constructor with many parameters?',
   '["Builder","telescoping constructor","immutable","fluent API","method chaining"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'System Design',
   'How would you design a URL shortener service? Describe the data model, key operations, and scalability considerations.',
   '["URL shortener","hashing","redirect","base62","distributed","collision"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Explain what @Transactional(propagation = REQUIRES_NEW) does and describe a use case where it is necessary.',
   '["REQUIRES_NEW","propagation","nested transaction","audit log","isolation"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'JPA/Hibernate',
   'What is the difference between Hibernate''s first-level cache and second-level cache? How do you enable second-level caching?',
   '["first-level cache","second-level cache","Session","SessionFactory","EhCache"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'What are the common strategies for versioning a REST API? Describe the trade-offs of each approach.',
   '["URI versioning","Accept header","query parameter","media type versioning","backward compatibility"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'What is eventual consistency and when is it an acceptable trade-off? Give an example of a real system that uses it.',
   '["eventual consistency","CAP theorem","BASE","strong consistency","DNS","replication lag"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'Explain the difference between horizontal and vertical scaling. What are the limitations and failure modes of each?',
   '["horizontal scaling","vertical scaling","load balancer","stateless","single point of failure"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Database',
   'What is a database index and how does it improve query performance? What are the costs of having too many indexes?',
   '["index","B-tree","query plan","write overhead","covering index","selectivity"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Testing',
   'Explain the difference between unit tests, integration tests, and end-to-end tests. When should you use each?',
   '["unit test","integration test","end-to-end","test pyramid","mock","Testcontainers"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'What is the CAP theorem and how does it constrain the design of distributed databases?',
   '["CAP theorem","consistency","availability","partition tolerance","trade-offs","distributed systems"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Security',
   'What is CORS and how would you configure a Spring Boot application to allow requests only from a specific frontend origin?',
   '["CORS","cross-origin","WebMvcConfigurer","@CrossOrigin","preflight","Access-Control-Allow-Origin"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Basics',
   'What are the benefits of using immutable objects in Java? Describe how you would design an immutable class.',
   '["immutable","final field","defensive copy","thread-safe","value object","records"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'How would you implement pagination in a Spring Data JPA repository? Describe the Pageable interface and how callers use it.',
   '["Pageable","Page","PageRequest","Spring Data JPA","pagination","sort"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'DevOps',
   'What is the difference between a Docker container and a virtual machine? When would you choose one over the other?',
   '["Docker","virtual machine","hypervisor","container","kernel","overhead"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Security',
   'Explain the OAuth 2.0 authorization code flow step by step, from the initial redirect to receiving an access token.',
   '["OAuth 2.0","authorization code","access token","refresh token","redirect URI","PKCE"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Debugging',
   'How would you debug a memory leak in a production Java application? Which tools would you use and what would you look for?',
   '["heap dump","VisualVM","JProfiler","memory leak","GC logs","OutOfMemoryError"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Java Collections',
   'Compare ArrayList and LinkedList in terms of time complexity for add, remove, and get-by-index operations.',
   '["ArrayList","LinkedList","O(1)","O(n)","random access","sequential access"]'::jsonb, NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Explain how Spring Boot auto-configuration works. How does @EnableAutoConfiguration decide which beans to create?',
   '["auto-configuration","@Conditional","spring.factories","@EnableAutoConfiguration","classpath detection","SPI"]'::jsonb, NOW(), NOW());

-- ============================================================
-- 5. Doc Questions (30)
-- ============================================================
INSERT INTO doc_question (id, category, question, created_at, updated_at) VALUES

  (gen_random_uuid(), 'Architecture',
   'Upload a document describing the high-level architecture of a recent project you worked on. Include a diagram if possible.', NOW(), NOW()),

  (gen_random_uuid(), 'Software Design',
   'Provide a UML class diagram for a simple e-commerce system including Product, Cart, Order, and Customer entities.', NOW(), NOW()),

  (gen_random_uuid(), 'Code Review',
   'Upload a code review of the provided Java service class. Document each issue found, its severity, and your suggested fix.', NOW(), NOW()),

  (gen_random_uuid(), 'Testing',
   'Provide a written test plan for a REST endpoint that creates a new user account, covering the happy path and key edge cases.', NOW(), NOW()),

  (gen_random_uuid(), 'System Design',
   'Upload a technical specification for a notification service that supports both email and SMS channels.', NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'Write and upload a document explaining your strategy for migrating a monolithic application to microservices over 12 months.', NOW(), NOW()),

  (gen_random_uuid(), 'System Design',
   'Provide a documented system design for a URL shortener that must handle 10,000 requests per second at peak load.', NOW(), NOW()),

  (gen_random_uuid(), 'Software Design',
   'Upload a sequence diagram showing the complete flow of a user login process using JWT authentication.', NOW(), NOW()),

  (gen_random_uuid(), 'Database',
   'Document your approach to diagnosing and optimising a slow database query on a 10-million-row table. Include EXPLAIN ANALYZE output if possible.', NOW(), NOW()),

  (gen_random_uuid(), 'Database',
   'Provide a written analysis (minimum 400 words) of the trade-offs between SQL and NoSQL databases for a real-time analytics use case.', NOW(), NOW()),

  (gen_random_uuid(), 'Design Patterns',
   'Upload a Java code sample demonstrating the Observer design pattern, with a brief written explanation of your design decisions.', NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'Write and upload a REST API specification in OpenAPI format for a task management application (tasks, users, projects).', NOW(), NOW()),

  (gen_random_uuid(), 'Security',
   'Provide a document describing best practices for securing a Spring Boot REST API, covering authentication, authorisation, and input validation.', NOW(), NOW()),

  (gen_random_uuid(), 'Operations',
   'Upload a written postmortem for a hypothetical production incident where a bad deployment caused a 30-minute service outage.', NOW(), NOW()),

  (gen_random_uuid(), 'DevOps',
   'Document your approach to building a CI/CD pipeline for a Spring Boot application, from commit to production deployment.', NOW(), NOW()),

  (gen_random_uuid(), 'Security',
   'Write and upload a technical comparison between JWT-based and traditional session-based authentication.', NOW(), NOW()),

  (gen_random_uuid(), 'Database',
   'Provide a safe migration strategy document for adding a NOT NULL column to a 50-million-row PostgreSQL table in a live system.', NOW(), NOW()),

  (gen_random_uuid(), 'Java Basics',
   'Upload a Java code sample demonstrating a thread-safe Singleton implementation. Briefly explain your choice of approach.', NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'Write and upload a document describing how you would implement request rate limiting in a REST API without third-party libraries.', NOW(), NOW()),

  (gen_random_uuid(), 'Software Design',
   'Provide a written analysis of the SOLID principles with code examples showing both a violation and a correct implementation for each principle.', NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'Upload a document comparing monolithic and microservices architectures specifically in the context of a developer assessment platform.', NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Write a technical document explaining how you would implement multi-level caching (local in-memory + distributed Redis) in a Spring Boot application.', NOW(), NOW()),

  (gen_random_uuid(), 'JPA/Hibernate',
   'Provide a written troubleshooting guide for the five most common JPA/Hibernate performance problems, with detection and fix strategies for each.', NOW(), NOW()),

  (gen_random_uuid(), 'REST',
   'Upload a document describing your approach to API error handling, including error response structure, HTTP status code selection, and logging.', NOW(), NOW()),

  (gen_random_uuid(), 'Research',
   'Provide a technology evaluation document comparing Spring MVC and Spring WebFlux for a high-concurrency REST API use case.', NOW(), NOW()),

  (gen_random_uuid(), 'System Design',
   'Provide a sequence diagram and written description for the assessment-taking flow, from token-based access through to final submission.', NOW(), NOW()),

  (gen_random_uuid(), 'Architecture',
   'Document your approach to implementing full-text search in a Spring Boot application. Compare database-level search vs a dedicated search engine.', NOW(), NOW()),

  (gen_random_uuid(), 'DevOps',
   'Upload a written explanation of how Docker networking works, covering bridge, host, and overlay network modes with a use case for each.', NOW(), NOW()),

  (gen_random_uuid(), 'Spring Boot',
   'Write a document describing how you would implement structured audit logging in a Spring Boot application (who changed what, when, and from where).', NOW(), NOW()),

  (gen_random_uuid(), 'Process',
   'Provide a code review guidelines document for a mid-sized Java development team, covering what to look for and how to give constructive feedback.', NOW(), NOW());

-- ============================================================
-- 6. Group Questions (30) with 2 follow-up text questions each
-- ============================================================
DO $$
DECLARE
  gq_id  UUID;
  fu1_id UUID;
  fu2_id UUID;

  gq_categories TEXT[] := ARRAY[
    'System Design',  'JPA/Hibernate',  'Security',      'Database',      'Operations',
    'Spring Boot',    'Architecture',   'Software Design','Database',      'Caching',
    'File Handling',  'Auditing',       'Performance',   'Bulk Processing','Scheduling',
    'Security',       'Assessment Domain','Assessment Domain','Security',  'Database',
    'Assessment Domain','Architecture', 'Security',      'Database',      'DevOps',
    'Feature Flags',  'Lombok/JPA',     'Spring Boot',   'Software Design','Architecture'
  ];

  gq_questions TEXT[] := ARRAY[
    'You are asked to design a REST API for a library management system that supports book inventory, member management, and loan tracking.',
    'A colleague submits a PR with a service method that fetches a list of orders and then, for each order, fetches the related line items in a separate query inside a loop.',
    'You are designing the JWT-based authentication flow for a new Spring Boot REST API from scratch.',
    'You discover that a critical database query is taking over 10 seconds on a 5-million-row table in a live system.',
    'A critical bug that was deployed to production is causing 500 errors on the checkout endpoint. The on-call alert has just fired.',
    'You are asked to add email notification support to an existing Spring Boot application without tightly coupling it to business logic.',
    'Your team is deciding whether to use a NoSQL or a relational database for a new high-volume analytics feature with a flexible schema.',
    'You are reviewing a service class that has grown to 800 lines and currently handles multiple unrelated concerns.',
    'A new business requirement asks for soft-deletion of records so that deleted data can be recovered for up to 30 days.',
    'You need to implement a rate-limiting feature to protect a public-facing API endpoint from abuse and excessive load.',
    'A client requests that all list API responses be paginated and include metadata such as total count, page number, and page size.',
    'You are asked to migrate a large monolithic application to microservices over a 12-month period.',
    'Your team needs to add multi-tenancy support to an existing single-tenant Spring Boot application.',
    'A junior developer has submitted a PR adding an in-memory caching layer to a frequently-read endpoint.',
    'You are building a file upload feature for document-based question types in this assessment platform.',
    'You are asked to add audit logging to capture all entity changes across a Spring Boot application.',
    'Your application is experiencing high heap memory usage and frequent full garbage collection pauses under load.',
    'A new feature requires importing 100,000 records from a CSV file into the database as part of a nightly batch job.',
    'You need to implement a scheduled job that sends reminder emails for candidate assessments that are due the following day.',
    'A security audit flags that a developer has used string concatenation to build a JPQL query that includes a user-supplied filter value.',
    'You are designing the system logic that ensures no candidate is ever shown the same question twice across multiple separate assessments.',
    'A candidate reports that their assessment timer expired but their in-progress responses were not saved or submitted.',
    'You need to implement role-based access control for an API with ADMIN, MARKER, and CANDIDATE roles in Spring Security.',
    'Your PostgreSQL queries are running slowly even though indexes exist on all the filtered columns.',
    'You are asked to add a health check endpoint to this Spring Boot application that is suitable for a Kubernetes liveness probe.',
    'You are building a feature flag system that enables specific new features only for selected user groups before a full rollout.',
    'A team member proposes annotating all JPA entity classes with Lombok''s @Data annotation to reduce boilerplate.',
    'You are building a search endpoint that accepts dynamic filter combinations, sorting options, and pagination.',
    'You are designing a notification system that must support email, SMS, and push notification channels through a single interface.',
    'A code review reveals that a developer is calling a Spring Data JPA repository directly from a REST controller, bypassing the service layer.'
  ];

  fu1_questions TEXT[] := ARRAY[
    'What HTTP methods and resource paths would you define for the Books, Members, and Loans resources?',
    'Identify the performance problem in this code and state the name of this well-known anti-pattern.',
    'Describe the JWT token structure and what claims you would include in the payload.',
    'What diagnostic steps would you take to investigate the slow query before making any changes?',
    'Describe your immediate steps to triage the incident and limit its impact on users.',
    'How would you structure the service layer so that notification logic stays decoupled from business logic?',
    'What questions would you ask the team to gather the information needed to make this decision?',
    'Identify which SOLID principle(s) are being violated and describe how you would approach the refactor.',
    'How would you implement soft-delete at the JPA entity and repository layer in Spring?',
    'What are the main algorithmic approaches to rate limiting and what are the trade-offs of each?',
    'Design the JSON response envelope for a paginated list of users, including all required metadata fields.',
    'What decomposition strategy would you use to identify the correct service boundaries for splitting the monolith?',
    'Compare the schema-per-tenant and shared-schema approaches to multi-tenancy. What are the trade-offs?',
    'What cache eviction strategies exist, and which would you recommend for a read-heavy product catalogue?',
    'What file type, size, and content validations would you implement before persisting the uploaded file?',
    'Compare using Hibernate Envers versus custom @PrePersist / @PreUpdate hooks for audit logging.',
    'What tools would you use to diagnose the heap usage problem and narrow down the root cause?',
    'How would you implement the bulk insert efficiently to avoid loading all 100,000 records into memory at once?',
    'How would you implement the scheduling using Spring''s @Scheduled annotation, and what cron expression would you use for "every day at 8 AM"?',
    'Explain why string-concatenated JPQL queries are a security risk and what the correct fix is.',
    'How would you query the database to identify which questions a specific candidate has previously been assigned?',
    'Describe how server-side auto-submission should work when the assessment time limit expires.',
    'How would you configure Spring Security to restrict a DELETE endpoint to the ADMIN role only?',
    'What is the EXPLAIN ANALYZE command in PostgreSQL and which parts of its output are most important to examine?',
    'What should a meaningful liveness health check verify beyond simply returning HTTP 200?',
    'How would you design the data model for storing feature flags and the user groups they target?',
    'What specific problems does @Data cause when applied to a JPA entity class?',
    'How would you implement dynamic filtering using Spring Data JPA Specifications or the Criteria API?',
    'Which design pattern would you use to support multiple notification channels behind a single interface?',
    'What architectural problems arise when a repository is called directly from a controller?'
  ];

  fu2_questions TEXT[] := ARRAY[
    'How would you handle the scenario where a member tries to borrow a book that is already on loan to someone else?',
    'Rewrite or describe the fix that eliminates this problem, and name the JPA feature that makes the fix possible.',
    'How would you handle token expiry and silent token refresh in the client and server implementations?',
    'What indexing strategies would you consider after diagnosing the query, and what trade-offs do they carry?',
    'What post-incident actions would you take to prevent this class of failure from recurring?',
    'How would you test the email notification service without sending real emails in unit and integration tests?',
    'Argue the case for one of the database options given these constraints: flexible schema, very high read volume, and data older than 90 days is rarely accessed.',
    'What risks are associated with this refactor and how would you manage them safely in a production codebase?',
    'What are the query implications of soft-delete for reporting and filtering, and how would you centralise the deleted filter to avoid bugs?',
    'Describe the token-bucket algorithm and outline how you would implement it in Java without an external library.',
    'How would you implement this pagination using Spring Data JPA''s Pageable interface and the Page return type?',
    'What challenges around data consistency would you anticipate when splitting the monolith, and how would you address them?',
    'How would you implement tenant isolation at the Spring Data JPA query level to prevent one tenant from seeing another''s data?',
    'How would you configure Spring''s @Cacheable so that different methods use different TTLs?',
    'How would you store the uploaded files and serve them back to authorised users securely?',
    'What information should each audit log entry capture to be useful for a compliance or forensic investigation?',
    'What common coding patterns in Java applications cause memory leaks, and how would you fix the most common ones?',
    'How would you handle partial failures — for example, if the import process fails at record 60,000?',
    'How would you ensure the job is reliable — specifically, what happens if the application restarts while the job is running?',
    'Beyond parameterised queries, what additional input validation would you apply to user-supplied search or filter values?',
    'How would you ensure this exclusion check remains correct and consistent when multiple assessments are being generated concurrently for the same candidate?',
    'What client-side safeguards would you implement to reduce the risk of losing a candidate''s in-progress responses?',
    'How would you write an integration test to verify that a CANDIDATE user cannot invoke a MARKER-only endpoint?',
    'What is a partial index in PostgreSQL and describe a concrete scenario in this platform where one would improve query performance?',
    'How does Spring Boot Actuator''s /actuator/health endpoint work and how would you add a custom health indicator for a downstream dependency?',
    'How would you evaluate a feature flag at the service layer efficiently without adding significant latency to every request?',
    'Which Lombok annotations would you use instead of @Data on JPA entities, and why is each choice safe for JPA proxies?',
    'What are the trade-offs between using JPA Specifications versus a custom @Query with JPQL for complex dynamic queries?',
    'How would you structure the code so that adding a new notification channel in the future requires no changes to existing channels?',
    'Describe the correct refactored design that follows the layered architecture and explain the concrete benefit it provides.'
  ];

  i INTEGER;
BEGIN
  FOR i IN 1..30 LOOP
    INSERT INTO group_question (id, category, question, keywords, ordered, created_at, updated_at)
    VALUES (gen_random_uuid(), gq_categories[i], gq_questions[i], NULL, false, NOW(), NOW())
    RETURNING id INTO gq_id;

    INSERT INTO text_question (id, category, question, keywords, created_at, updated_at)
    VALUES (gen_random_uuid(), gq_categories[i], fu1_questions[i], NULL, NOW(), NOW())
    RETURNING id INTO fu1_id;

    INSERT INTO text_question (id, category, question, keywords, created_at, updated_at)
    VALUES (gen_random_uuid(), gq_categories[i], fu2_questions[i], NULL, NOW(), NOW())
    RETURNING id INTO fu2_id;

    INSERT INTO group_question_follow_up (group_id, question_id, display_order)
    VALUES (gq_id, fu1_id, 0), (gq_id, fu2_id, 1);
  END LOOP;
END;
$$;
