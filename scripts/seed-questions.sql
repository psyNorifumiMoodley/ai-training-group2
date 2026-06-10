-- ============================================================
-- seed-questions.sql  —  Clear and re-seed questions (Phase 6 schema)
--
-- Wipes all question banks, questions, and bank associations, then
-- seeds questions across 12 named question banks.
-- Question types: MCQ, MCQ_PLUS, TEXT, DOC, GROUP
--
-- NOTE: assessment_question_link is cleared as a consequence —
-- existing assessments will no longer have assigned questions.
-- User and assessment records are not affected.
--
-- Run:
--   PowerShell:
--     Get-Content scripts\seed-questions.sql |
--       docker exec -i ai-training-group2-postgres-1 psql -U dap -d dap
--   Bash:
--     docker exec -i ai-training-group2-postgres-1 psql -U dap -d dap \
--       < scripts/seed-questions.sql
-- ============================================================

-- ============================================================
-- 1. Clear question data
-- ============================================================
TRUNCATE
  question_question_bank,
  group_question_child,
  assessment_question_link,
  mcq_plus_question,
  mcq_question,
  text_question,
  doc_question,
  group_question,
  assessment_question,
  question_bank
CASCADE;

-- ============================================================
-- 2. Question Banks (6)
-- ============================================================
INSERT INTO question_bank (id, name, created_at, updated_at) VALUES
  ('bb000001-0000-0000-0000-000000000001', 'Java Core',                    NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000002', 'Spring & JPA',                 NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000003', 'SQL & Databases',              NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000004', 'System Design',                NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000005', 'Software Fundamentals',        NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000006', 'DevOps & Security',            NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000007', 'Angular & Frontend',           NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000008', 'TypeScript',                   NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000009', 'Testing & Quality',            NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000010', 'Cloud & Infrastructure',       NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000011', 'Data Structures & Algorithms', NOW(), NOW()),
  ('bb000001-0000-0000-0000-000000000012', 'Agile & Best Practices',       NOW(), NOW());

-- ============================================================
-- 3. MCQ Questions (40)
-- Each statement inserts one question then immediately maps it to a bank.
-- ============================================================

-- === Java Core (8) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which keyword prevents a Java class from being subclassed?',
   '["final","static","abstract","sealed"]'::jsonb, '["final"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the default value of an uninitialized int field in a Java class?',
   '["0","null","-1","It causes a compile error"]'::jsonb, '["0"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which of the following correctly describes the String class in Java?',
   '["It is final and cannot be subclassed","It is abstract","It implements Comparable only","It is mutable"]'::jsonb,
   '["It is final and cannot be subclassed"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which interface does HashMap directly implement?',
   '["Map","List","Set","Collection"]'::jsonb, '["Map"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the instanceof operator check?',
   '["Whether an object is an instance of a class or interface at runtime","Whether two references point to the same object","Whether a class implements an interface at compile time","Whether a method exists on an object"]'::jsonb,
   '["Whether an object is an instance of a class or interface at runtime"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which access modifier makes a member visible only within its own package (and to no other package)?',
   '["No modifier (package-private)","protected","private","public"]'::jsonb,
   '["No modifier (package-private)"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does autoboxing refer to in Java?',
   '["Automatic conversion between primitive types and their wrapper classes","Automatic memory allocation on the heap","Automatic garbage collection of unreachable objects","Automatic serialisation of Java objects"]'::jsonb,
   '["Automatic conversion between primitive types and their wrapper classes"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which statement about Java interfaces (since Java 8) is correct?',
   '["Interfaces can have default methods with implementations","Interfaces cannot contain any methods","Interfaces can only extend one other interface","Interfaces cannot have static methods"]'::jsonb,
   '["Interfaces can have default methods with implementations"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

-- === Spring & JPA (8) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the @RestController annotation imply in Spring?',
   '["Both @Controller and @ResponseBody","Only @Controller","Only @ResponseBody","@Service and @Controller combined"]'::jsonb,
   '["Both @Controller and @ResponseBody"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the primary purpose of @Transactional in Spring?',
   '["To define transaction boundaries for service methods","To mark a class as a Spring bean","To enable dependency injection","To configure HTTP request mappings"]'::jsonb,
   '["To define transaction boundaries for service methods"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which Spring Data JPA method returns an Optional<T>?',
   '["findById()","getOne()","findAll()","count()"]'::jsonb,
   '["findById()"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does FetchType.LAZY mean in a JPA association?',
   '["The association is loaded only when explicitly accessed","The association is loaded with the parent entity","The association is never loaded","The association is loaded asynchronously in a background thread"]'::jsonb,
   '["The association is loaded only when explicitly accessed"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does spring.jpa.hibernate.ddl-auto=validate do at startup?',
   '["Validates that the DB schema matches entity mappings; fails if there is a mismatch","Creates the schema from entity mappings on every startup","Drops and recreates the schema on every startup","Does nothing — schema validation is always manual"]'::jsonb,
   '["Validates that the DB schema matches entity mappings; fails if there is a mismatch"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does @Column(nullable = false) add to the mapped column?',
   '["A NOT NULL constraint","A UNIQUE constraint","A primary key constraint","A default value of empty string"]'::jsonb,
   '["A NOT NULL constraint"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which JPA feature is the recommended solution to the N+1 query problem?',
   '["@EntityGraph or JOIN FETCH in JPQL","@Transactional(readOnly = true) on all methods","FetchType.EAGER on every association","@CacheEvict on the parent repository"]'::jsonb,
   '["@EntityGraph or JOIN FETCH in JPQL"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the default bean scope in a Spring application context?',
   '["Singleton","Prototype","Request","Session"]'::jsonb,
   '["Singleton"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

-- === SQL & Databases (8) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the difference between WHERE and HAVING in SQL?',
   '["WHERE filters rows before grouping; HAVING filters groups after aggregation","WHERE filters rows after grouping; HAVING filters before","They are interchangeable","WHERE works only on text columns; HAVING works on all types"]'::jsonb,
   '["WHERE filters rows before grouping; HAVING filters groups after aggregation"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does a FULL OUTER JOIN return?',
   '["All rows from both tables with NULLs where there is no match","Only rows that match in both tables","All rows from the left table with NULLs for unmatched right rows","All rows from the right table with NULLs for unmatched left rows"]'::jsonb,
   '["All rows from both tables with NULLs where there is no match"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the COALESCE function return?',
   '["The first non-NULL value from its arguments","The last non-NULL value","NULL if any argument is NULL","The average of its arguments"]'::jsonb,
   '["The first non-NULL value from its arguments"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the ''I'' in ACID stand for?',
   '["Isolation","Integrity","Idempotency","Integration"]'::jsonb,
   '["Isolation"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which property distinguishes a PRIMARY KEY from a UNIQUE constraint?',
   '["A PRIMARY KEY column cannot be NULL; a UNIQUE column can","UNIQUE constraints are faster to look up","PRIMARY KEY allows one duplicate value","There is no functional difference"]'::jsonb,
   '["A PRIMARY KEY column cannot be NULL; a UNIQUE column can"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does EXPLAIN ANALYZE do in PostgreSQL?',
   '["Executes the query and shows the actual execution plan with real timing","Shows the estimated execution plan without running the query","Automatically rewrites the query for better performance","Displays only the indexes used by the query"]'::jsonb,
   '["Executes the query and shows the actual execution plan with real timing"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does SELECT DISTINCT do?',
   '["Removes duplicate rows from the result set","Sorts the result set alphabetically","Selects only indexed columns","Limits the number of rows returned to one per group"]'::jsonb,
   '["Removes duplicate rows from the result set"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which SQL clause is used to sort a query result set?',
   '["ORDER BY","SORT BY","GROUP BY","RANK BY"]'::jsonb,
   '["ORDER BY"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

-- === System Design (6) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which HTTP method is idempotent but NOT safe?',
   '["PUT","GET","POST","DELETE"]'::jsonb,
   '["PUT"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does HTTP status code 201 indicate?',
   '["A resource was successfully created","The request was accepted but not yet processed","The resource was not found","There was a server error"]'::jsonb,
   '["A resource was successfully created"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does ''stateless'' mean in the context of a REST API?',
   '["The server does not store any client session state between requests","The server stores state only in a database","Clients cannot send any state to the server","Responses always contain identical data regardless of the caller"]'::jsonb,
   '["The server does not store any client session state between requests"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does horizontal scaling mean?',
   '["Adding more instances of a service to distribute load","Upgrading a single server to a more powerful machine","Increasing database storage capacity only","Adding more CPU cores to an existing machine"]'::jsonb,
   '["Adding more instances of a service to distribute load"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does ''eventual consistency'' mean in a distributed system?',
   '["All replicas will converge to the same value given no new updates","All replicas are always immediately consistent","Only the primary node is consistent; replicas may lag permanently","The system guarantees consistency only if errors are rare"]'::jsonb,
   '["All replicas will converge to the same value given no new updates"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the CAP theorem state about a distributed system?',
   '["It can guarantee at most two of Consistency, Availability, and Partition Tolerance","All three properties can always be achieved simultaneously","Consistency and Availability are mutually exclusive","Partition Tolerance is optional and can always be dropped"]'::jsonb,
   '["It can guarantee at most two of Consistency, Availability, and Partition Tolerance"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

-- === Software Fundamentals (5) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the time complexity of binary search on a sorted array?',
   '["O(log n)","O(n)","O(n log n)","O(1)"]'::jsonb,
   '["O(log n)"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which design pattern ensures only one instance of a class exists in the application?',
   '["Singleton","Factory","Observer","Decorator"]'::jsonb,
   '["Singleton"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the element ordering of a Stack data structure?',
   '["LIFO — Last In, First Out","FIFO — First In, First Out","Random order","Sorted ascending order"]'::jsonb,
   '["LIFO — Last In, First Out"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the average-case time complexity of a HashMap get(key) lookup?',
   '["O(1)","O(log n)","O(n)","O(n²)"]'::jsonb,
   '["O(1)"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the purpose of the Observer design pattern?',
   '["To define a one-to-many dependency so dependents are notified when one object changes state","To create objects without specifying their concrete class","To add behaviour to an object at runtime by wrapping it","To provide a simplified interface to a complex subsystem"]'::jsonb,
   '["To define a one-to-many dependency so dependents are notified when one object changes state"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

-- === DevOps & Security (5) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which hashing algorithm is most suitable for securely storing user passwords?',
   '["BCrypt","MD5","SHA-1","Base64"]'::jsonb,
   '["BCrypt"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the EXPOSE instruction in a Dockerfile do?',
   '["Documents which port the container listens on at runtime","Publishes the port to the host machine automatically","Opens a firewall rule on the host","Configures the upstream load balancer"]'::jsonb,
   '["Documents which port the container listens on at runtime"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does JWT stand for?',
   '["JSON Web Token","Java Web Template","JavaScript Web Transfer","JSON Workflow Token"]'::jsonb,
   '["JSON Web Token"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'During which Maven lifecycle phase are unit tests compiled and executed?',
   '["test","verify","compile","package"]'::jsonb,
   '["test"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which protocol does HTTPS use to encrypt communication between client and server?',
   '["TLS","SSH","IPSec","SFTP"]'::jsonb,
   '["TLS"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

-- ============================================================
-- 4. MCQ_PLUS Questions (20)
-- Each has an MCQ section + a follow-up text question.
-- ============================================================

-- === Java Core (4) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which Java collection provides O(1) average-case lookup by key?',
   '["HashMap","ArrayList","LinkedList","TreeMap"]'::jsonb,
   '["HashMap"]'::jsonb,
   'Explain how HashMap handles hash collisions internally and describe what happens to lookup performance as collisions increase.',
   '["hash collision","chaining","linked list","tree","load factor","rehashing","bucket","O(n)"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does the volatile keyword guarantee in Java multi-threaded code?',
   '["Visibility — all threads see the latest written value immediately","Atomicity — operations on the variable are fully thread-safe","Mutual exclusion — only one thread can access the variable at a time","Ordering — volatile variables are always processed before other variables"]'::jsonb,
   '["Visibility — all threads see the latest written value immediately"]'::jsonb,
   'Explain why volatile alone is insufficient to make a compound operation such as counter++ thread-safe, and describe a correct alternative.',
   '["visibility","atomicity","read-modify-write","race condition","synchronized","AtomicInteger","volatile","happens-before"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which of the following Java classes is immutable?',
   '["String","StringBuilder","ArrayList","HashMap"]'::jsonb,
   '["String"]'::jsonb,
   'Describe the key design decisions that make String immutable in Java and explain the memory and thread-safety benefits this brings.',
   '["final class","final fields","no setters","String pool","interning","thread safety","defensive copy","immutability"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the result of Integer.valueOf(127) == Integer.valueOf(127) in Java?',
   '["true — the Integer cache returns the same cached instance","false — valueOf always creates a new Integer object","Depends on JVM implementation and flags","A compilation error because == cannot compare Integer objects"]'::jsonb,
   '["true — the Integer cache returns the same cached instance"]'::jsonb,
   'Explain the Java Integer cache mechanism, state its default range, and explain why Integer.valueOf(128) == Integer.valueOf(128) evaluates to false.',
   '["integer cache","-128 to 127","flyweight","autoboxing","reference equality","object pool","128","new instance"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

-- === Spring & JPA (4) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the default scope of a Spring bean?',
   '["Singleton — one shared instance per application context","Prototype — a new instance on every injection","Request — one instance per HTTP request","Session — one instance per HTTP session"]'::jsonb,
   '["Singleton — one shared instance per application context"]'::jsonb,
   'Describe a scenario where changing the bean scope to Prototype is necessary, and explain the risk of injecting a Prototype-scoped bean directly into a Singleton bean.',
   '["singleton","prototype","stateful","stateless","scoped proxy","method injection","ObjectProvider","application context"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does orphanRemoval = true do in a JPA @OneToMany relationship?',
   '["Deletes child entities that are removed from the parent collection","Cascades all persistence operations from parent to children","Prevents deletion of the parent if it still has children","Sets child foreign key columns to NULL when removed from the collection"]'::jsonb,
   '["Deletes child entities that are removed from the parent collection"]'::jsonb,
   'Explain the difference between orphanRemoval=true and CascadeType.REMOVE. Describe a situation where CascadeType.REMOVE alone would be insufficient.',
   '["orphanRemoval","CascadeType.REMOVE","collection","disconnect","aggregate root","delete","unlink","parent"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which @Transactional propagation type always starts a completely new independent transaction?',
   '["REQUIRES_NEW","MANDATORY","NESTED","SUPPORTS"]'::jsonb,
   '["REQUIRES_NEW"]'::jsonb,
   'Describe a real-world use case where REQUIRES_NEW is necessary (e.g., audit logging) and explain what happens to the outer transaction while the inner one executes.',
   '["REQUIRES_NEW","suspend","outer transaction","inner transaction","audit log","independent commit","rollback isolation","nested"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does @EntityGraph do in Spring Data JPA?',
   '["Eagerly loads specified associations for a query without changing the global FetchType","Makes all associations eagerly loaded globally across the entity","Caches the entity in the second-level cache for subsequent requests","Prevents LazyInitializationException by disabling lazy loading entirely"]'::jsonb,
   '["Eagerly loads specified associations for a query without changing the global FetchType"]'::jsonb,
   'Compare @EntityGraph with JOIN FETCH in JPQL. What is the key structural difference and when would you prefer one over the other?',
   '["EntityGraph","JOIN FETCH","N+1","fetch strategy","ad-hoc","named graph","Cartesian product","DISTINCT","JPQL"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

-- === SQL & Databases (4) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does SELECT FOR UPDATE do in SQL?',
   '["Acquires a row-level exclusive lock on the selected rows","Marks the selected rows as read-only for the current session","Prevents other sessions from reading the selected rows entirely","Applies optimistic locking without blocking concurrent readers"]'::jsonb,
   '["Acquires a row-level exclusive lock on the selected rows"]'::jsonb,
   'Describe how a deadlock can arise when two concurrent transactions both use SELECT FOR UPDATE on overlapping rows. Explain at least one strategy to prevent it.',
   '["deadlock","lock ordering","row-level lock","wait graph","consistent ordering","NOWAIT","SKIP LOCKED","transaction","rollback"]'::jsonb,
   5, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which normal form eliminates transitive dependencies?',
   '["Third Normal Form (3NF)","First Normal Form (1NF)","Second Normal Form (2NF)","Boyce-Codd Normal Form (BCNF)"]'::jsonb,
   '["Third Normal Form (3NF)"]'::jsonb,
   'Describe a scenario where deliberately denormalising a table is justified, and explain what data integrity trade-offs you are accepting by doing so.',
   '["denormalisation","read performance","update anomaly","redundancy","reporting","OLAP","materialised view","normalisation","3NF"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the main difference between TRUNCATE and DELETE in PostgreSQL?',
   '["TRUNCATE removes all rows without scanning them row-by-row, making it faster for full clears","TRUNCATE logs every deleted row; DELETE does not","TRUNCATE supports a WHERE clause; DELETE does not","TRUNCATE is non-transactional in PostgreSQL; DELETE is always transactional"]'::jsonb,
   '["TRUNCATE removes all rows without scanning them row-by-row, making it faster for full clears"]'::jsonb,
   'Explain the transactional behaviour of TRUNCATE vs DELETE in PostgreSQL and describe a situation where choosing DELETE over TRUNCATE is important.',
   '["TRUNCATE","DELETE","MVCC","transaction","rollback","WHERE clause","foreign key","RESTART IDENTITY","MDL","lock"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is a partial index in PostgreSQL?',
   '["An index built over a subset of rows that match a specified condition","An index that covers only some columns of a multi-column key","An index that is still being built concurrently and is not yet complete","An index created on a nullable column to handle sparse data"]'::jsonb,
   '["An index built over a subset of rows that match a specified condition"]'::jsonb,
   'Describe a concrete use case where a partial index offers significant space or performance benefit over a full index on the same column.',
   '["partial index","WHERE clause","active rows","status filter","low cardinality","IS NOT NULL","index size","selectivity","soft delete"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

-- === System Design (4) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which architectural pattern separates read models from write models into distinct processing flows?',
   '["CQRS — Command Query Responsibility Segregation","Event Sourcing","Saga pattern","Hexagonal Architecture"]'::jsonb,
   '["CQRS — Command Query Responsibility Segregation"]'::jsonb,
   'Explain the key trade-off introduced by CQRS compared to using a single unified model, and describe the types of systems where this trade-off is worthwhile.',
   '["CQRS","read model","write model","eventual consistency","complexity","reporting","separate storage","event-driven","synchronisation"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the purpose of the Circuit Breaker pattern in microservices?',
   '["To stop calls to a failing downstream service and give it time to recover","To route requests to the fastest available service instance","To encrypt all inter-service communication","To balance load evenly across all service instances"]'::jsonb,
   '["To stop calls to a failing downstream service and give it time to recover"]'::jsonb,
   'Describe the three states of a circuit breaker (Closed, Open, Half-Open) and explain what event triggers each state transition.',
   '["closed","open","half-open","failure threshold","timeout","probe request","fallback","Resilience4j","cascading failure","recovery"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the primary purpose of a message queue in a distributed system?',
   '["To decouple producers and consumers by enabling asynchronous communication","To synchronise distributed transactions atomically","To persist data permanently as the primary data store","To load-balance HTTP requests across service instances"]'::jsonb,
   '["To decouple producers and consumers by enabling asynchronous communication"]'::jsonb,
   'Describe the difference between at-least-once and exactly-once message delivery guarantees, and explain the application-level handling required for each.',
   '["at-least-once","exactly-once","idempotency","deduplication","ack","retry","offset","message broker","consumer","producer"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the primary function of a CDN (Content Delivery Network)?',
   '["To serve content from geographically distributed edge servers close to the user","To provide distributed database storage across regions","To globally load-balance API requests across data centres","To cache API responses directly in the browser"]'::jsonb,
   '["To serve content from geographically distributed edge servers close to the user"]'::jsonb,
   'Describe which types of content are most and least suited for CDN caching, and explain why dynamic personalised content is poorly suited.',
   '["edge server","static assets","cache-control","TTL","origin server","dynamic content","personalisation","cache invalidation","CDN","stale"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

-- === Software Fundamentals (2) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which design pattern creates objects without specifying their exact concrete class?',
   '["Factory Method","Singleton","Decorator","Adapter"]'::jsonb,
   '["Factory Method"]'::jsonb,
   'Explain when you would use the Factory Method pattern instead of directly instantiating an object with new. What specific problem does it solve?',
   '["Factory Method","open/closed principle","subclass","decoupling","polymorphism","Abstract Factory","dependency injection","new keyword","interface"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which sorting algorithm is both stable and guarantees O(n log n) in all cases?',
   '["Merge Sort","Quick Sort","Heap Sort","Insertion Sort"]'::jsonb,
   '["Merge Sort"]'::jsonb,
   'Explain what ''stable sort'' means and give a real-world example where a stable sorting algorithm is required.',
   '["stable sort","relative order","equal elements","composite sort","secondary key","spreadsheet","name and age","unstable","sort stability"]'::jsonb,
   3, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

-- === DevOps & Security (2) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What class of attack do parameterised SQL queries (prepared statements) prevent?',
   '["SQL injection","Cross-Site Scripting (XSS)","Cross-Site Request Forgery (CSRF)","Brute-force login attacks"]'::jsonb,
   '["SQL injection"]'::jsonb,
   'Explain how a SQL injection attack works with a concrete example, and describe at a technical level why parameterised queries prevent it.',
   '["SQL injection","prepared statement","parameter binding","escape","query structure","attacker input","string concatenation","malicious payload","unsanitized input"]'::jsonb,
   5, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the primary purpose of a load balancer?',
   '["To distribute incoming requests across multiple backend server instances","To cache static content at the network edge","To encrypt traffic between the client and the server","To manage the database connection pool across services"]'::jsonb,
   '["To distribute incoming requests across multiple backend server instances"]'::jsonb,
   'Describe at least two different load-balancing algorithms (e.g., round-robin, least-connections) and explain a scenario where each is the better choice.',
   '["round-robin","least connections","IP hash","weighted","sticky sessions","health check","upstream","server pool","algorithm","affinity"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

-- ============================================================
-- 5. TEXT Questions (40)
-- ============================================================

-- === Java Core (8) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the difference between checked and unchecked exceptions in Java. When would you choose to use each?',
   '["checked exception","unchecked exception","RuntimeException","IOException","throws declaration","try-catch","compiler","recover"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe how garbage collection works in the JVM. Explain the different heap generations and when each is collected.',
   '["garbage collection","JVM","heap","young generation","old generation","eden space","survivor space","major GC","minor GC","GC roots","reachability"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What are the benefits of using immutable objects in Java? Describe how you would correctly design an immutable class.',
   '["immutability","thread safety","final class","final fields","defensive copy","no setters","String","value object","cache","hashCode"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Compare ArrayList and LinkedList in terms of time complexity for add, remove, and random-access operations. When would you choose each?',
   '["ArrayList","LinkedList","O(1)","O(n)","random access","insertion","deletion","cache locality","memory","iterator"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the difference between == and .equals() in Java. When should you override .equals(), and what rules must a correct implementation follow?',
   '["reference equality","value equality","equals","hashCode","contract","reflexive","symmetric","transitive","null-safe","Objects.equals"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the Java memory model. What is the difference between stack memory and heap memory, and what types of data are stored in each?',
   '["stack","heap","thread","local variable","object reference","primitive","method frame","garbage collector","OutOfMemoryError","StackOverflowError"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain how Java generics work and what type erasure means at the bytecode level. What limitations does type erasure impose on generic code?',
   '["generics","type erasure","compile time","bytecode","unchecked cast","bounded wildcard","reification","instanceof","overloading","List<T>"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the Java Stream API. When would you prefer streams over traditional for-loops? What are the trade-offs to consider?',
   '["Stream","lambda","functional","lazy evaluation","terminal operation","intermediate operation","pipeline","parallelStream","readability","performance"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

-- === Spring & JPA (8) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the differences between @Component, @Service, and @Repository annotations in Spring. Are they interchangeable?',
   '["@Component","@Service","@Repository","stereotype annotation","exception translation","AOP","semantics","PersistenceExceptionTranslationPostProcessor"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the N+1 query problem in JPA? Describe a concrete scenario where it occurs and explain at least two strategies to resolve it.',
   '["N+1","lazy loading","JOIN FETCH","@EntityGraph","batch fetching","Hibernate","JPQL","FetchType","association","SELECT N"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain what @Transactional(propagation = REQUIRES_NEW) does and describe a concrete use case where it is necessary.',
   '["REQUIRES_NEW","propagation","suspend","outer transaction","inner transaction","audit log","rollback","commit","independent","nested"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'How would you implement pagination in a Spring Data JPA repository? Describe the Pageable interface and explain how a REST controller should use it.',
   '["Pageable","PageRequest","Page","sort","page number","page size","Slice","controller","@PageableDefault","content","totalElements"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe how Spring Security JWT authentication works from the moment a user submits credentials to the point they receive a signed token.',
   '["UsernamePasswordAuthenticationToken","AuthenticationManager","UserDetailsService","JWT","signing key","claims","response","Bearer","filter","stateless"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the difference between Hibernate first-level cache and second-level cache? How do you configure and invalidate the second-level cache?',
   '["first-level cache","second-level cache","session","session factory","@Cache","region","Ehcache","eviction","@CacheEvict","query cache"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the Spring bean lifecycle from instantiation to destruction. Which annotations or interfaces let you hook into key lifecycle phases?',
   '["instantiation","dependency injection","@PostConstruct","InitializingBean","afterPropertiesSet","@PreDestroy","DisposableBean","BeanPostProcessor","lifecycle","context refresh"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'How would you implement a Spring Boot search endpoint that accepts dynamic filter combinations, sorting options, and pagination? Describe the technical approach.',
   '["Specification","JpaSpecificationExecutor","Criteria API","Pageable","filter","sort","dynamic query","@RequestParam","Predicate","CriteriaBuilder"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000002' FROM q;

-- === SQL & Databases (8) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the differences between INNER JOIN, LEFT JOIN, and FULL OUTER JOIN. Provide a concrete example for each.',
   '["INNER JOIN","LEFT JOIN","FULL OUTER JOIN","matching rows","NULL","result set","ON clause","unmatched","join type","Cartesian product"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is a database transaction? Explain each of the four ACID properties with a real-world example.',
   '["transaction","atomicity","consistency","isolation","durability","rollback","commit","all-or-nothing","bank transfer","concurrent access"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is a database index and how does it improve query performance? What are the costs and risks of having too many indexes on a table?',
   '["index","B-tree","lookup","seek","sequential scan","write overhead","storage","maintenance","query planner","covering index","insert performance"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the differences between optimistic and pessimistic locking. When would you choose each approach?',
   '["optimistic locking","pessimistic locking","version column","@Version","SELECT FOR UPDATE","conflict","retry","throughput","low contention","high contention"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe a safe migration strategy for adding a NOT NULL column to a 50-million-row PostgreSQL table in a live production system without downtime.',
   '["NOT NULL","backfill","three-phase migration","DEFAULT","nullable first","CHECK constraint","NOT VALID","validate","online migration","downtime"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What are database transaction isolation levels? Describe at least three levels, the anomalies each prevents, and the trade-offs between them.',
   '["READ UNCOMMITTED","READ COMMITTED","REPEATABLE READ","SERIALIZABLE","dirty read","non-repeatable read","phantom read","isolation level","throughput","lock contention"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Compare SQL and NoSQL databases. Describe three scenarios where you would choose each type and justify your reasoning.',
   '["SQL","NoSQL","relational","schema","ACID","eventual consistency","document store","key-value","horizontal scaling","query flexibility","joins"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is query plan analysis? Describe what you look for in a PostgreSQL EXPLAIN ANALYZE output when diagnosing a slow query.',
   '["EXPLAIN ANALYZE","query plan","sequential scan","index scan","cost","rows","actual time","nested loop","hash join","buffers","recheck condition"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000003' FROM q;

-- === System Design (6) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'How would you design a URL shortener service? Describe the data model, key operations, and at least two scalability considerations.',
   '["URL shortener","short code","redirect","data model","hash","collision","cache","CDN","analytics","scale","read-heavy","base62"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the CAP theorem and how it constrains distributed database design. Give a concrete example for each of the three valid system combinations.',
   '["CAP theorem","consistency","availability","partition tolerance","CP","AP","CA","Cassandra","ZooKeeper","network partition","trade-off"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe a realistic strategy for migrating a monolithic Spring Boot application to microservices over 12 months. What are the key milestones?',
   '["strangler fig","bounded context","service decomposition","API gateway","event-driven","data isolation","contract testing","incremental","deployment","rollback"]'::jsonb,
   10, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is eventual consistency and when is it an acceptable trade-off? Give a real-world example of a system that uses it.',
   '["eventual consistency","strong consistency","replication lag","BASE","DNS","shopping cart","conflict resolution","last-write-wins","vector clock","availability"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the differences between horizontal and vertical scaling. What are the failure modes and practical upper limits of each approach?',
   '["horizontal scaling","vertical scaling","scale out","scale up","stateless","load balancer","single point of failure","cost","ceiling","commodity hardware"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe how you would implement rate limiting on a public-facing REST API endpoint. Which algorithm would you choose and why?',
   '["rate limiting","token bucket","sliding window","fixed window","Redis","HTTP 429","throttle","API key","distributed","burst","Lua script"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

-- === Software Fundamentals (5) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the Builder design pattern. Why would you prefer it over a constructor with many parameters? Provide a Java example.',
   '["Builder","telescoping constructor","fluent interface","immutable","optional parameters","readability","validation","Lombok @Builder","inner static class"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What are the SOLID principles? Briefly explain each one and give a concrete Java example of where you would apply it.',
   '["Single Responsibility","Open/Closed","Liskov Substitution","Interface Segregation","Dependency Inversion","SOLID","OOP","design principle","refactoring"]'::jsonb,
   10, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Compare the Observer and Strategy design patterns. Describe a use case for each and explain what structurally distinguishes them.',
   '["Observer","Strategy","behavioural pattern","publish-subscribe","event listener","algorithm","context","interface","encapsulate","interchangeable"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the difference between Breadth-First Search and Depth-First Search. When would you choose each approach in a real application?',
   '["BFS","DFS","queue","stack","level order","shortest path","connected component","cycle detection","tree traversal","visited set"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the difference between a tree and a graph? Describe three practical use cases for each data structure.',
   '["tree","graph","acyclic","cycle","root","parent-child","directed","undirected","weighted","file system","social network","dependency graph"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000005' FROM q;

-- === DevOps & Security (5) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is CORS? How would you configure a Spring Boot application to allow requests only from a specific frontend origin?',
   '["CORS","cross-origin","preflight","OPTIONS","Allow-Origin","WebMvcConfigurer","addCorsMappings","@CrossOrigin","same-origin policy","credentials"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the OAuth 2.0 Authorization Code flow step by step, from the initial redirect to the application receiving an access token.',
   '["OAuth 2.0","authorization code","redirect URI","code exchange","client credentials","access token","refresh token","PKCE","authorization server","resource server"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'How would you diagnose a memory leak in a production Java application? Which tools would you use and what would you look for?',
   '["heap dump","memory leak","jmap","VisualVM","Eclipse MAT","retained heap","GC overhead","OutOfMemoryError","reference chain","unreachable objects"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the difference between a Docker container and a virtual machine? When would you choose one over the other?',
   '["container","VM","hypervisor","kernel","image","isolation","resource overhead","startup time","Dockerfile","bare metal","namespace","cgroup"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe how you would implement structured audit logging in a Spring Boot application to capture who changed what, when, and from where.',
   '["audit log","@PrePersist","@PreUpdate","Spring Data Auditing","AOP","SecurityContextHolder","entity listener","MDC","correlation ID","immutable log"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000006' FROM q;

-- ============================================================
-- 6. Additional MCQ — new banks (banks 7-12)
-- ============================================================

-- === Angular & Frontend (5) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does Angular''s OnPush change detection strategy do?',
   '["Checks for changes only when an input reference changes or an async event fires within the component","Checks every component on every browser event","Disables change detection entirely until manually triggered","Only runs change detection on the root component"]'::jsonb,
   '["Checks for changes only when an input reference changes or an async event fires within the component"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which RxJS operator transforms each emitted value into a new inner observable and cancels previous inner observables on each new emission?',
   '["switchMap","mergeMap","concatMap","exhaustMap"]'::jsonb,
   '["switchMap"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the purpose of the Angular Router''s CanActivate guard?',
   '["To prevent navigation to a route if a condition is not met","To preload data before a route activates","To prevent navigation away from a route","To lazy-load a feature module on demand"]'::jsonb,
   '["To prevent navigation to a route if a condition is not met"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the Angular async pipe do?',
   '["Subscribes to an Observable or Promise in the template and automatically unsubscribes on component destruction","Marks a method as asynchronous","Converts a callback to an Observable","Delays template rendering until the Observable emits"]'::jsonb,
   '["Subscribes to an Observable or Promise in the template and automatically unsubscribes on component destruction"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which CSS layout model is best suited for one-dimensional layouts (a single row or column)?',
   '["Flexbox","CSS Grid","Float layout","Table layout"]'::jsonb,
   '["Flexbox"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

-- === TypeScript (4) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does TypeScript''s strict mode enable?',
   '["strictNullChecks, noImplicitAny, strictFunctionTypes, and several other checks","Only strictNullChecks","Only type checking on function return types","Disables JavaScript interop"]'::jsonb,
   '["strictNullChecks, noImplicitAny, strictFunctionTypes, and several other checks"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the difference between unknown and any in TypeScript?',
   '["unknown requires type narrowing before use; any bypasses all type checks","They are identical","unknown is deprecated in newer versions","any is stricter than unknown"]'::jsonb,
   '["unknown requires type narrowing before use; any bypasses all type checks"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the keyof operator do in TypeScript?',
   '["Produces a union type of all keys of a given type","Returns the number of keys in an object","Creates an index signature for a type","Validates that a string is a valid property name at runtime"]'::jsonb,
   '["Produces a union type of all keys of a given type"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is a discriminated union in TypeScript?',
   '["A union of types that each share a common literal property used to narrow the type","A union where all members must be primitive types","A union that can only contain two types","A union type that excludes null and undefined"]'::jsonb,
   '["A union of types that each share a common literal property used to narrow the type"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

-- === Testing & Quality (5) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the Testcontainers library provide for JVM integration tests?',
   '["Lightweight, throwaway Docker containers for databases and other infrastructure","An in-memory database that mimics PostgreSQL behaviour","A mocking framework for Spring beans","A code coverage measurement tool"]'::jsonb,
   '["Lightweight, throwaway Docker containers for databases and other infrastructure"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the @MockBean annotation do in a Spring Boot test?',
   '["Replaces a bean in the Spring application context with a Mockito mock","Creates a partial mock of a real bean","Disables a bean from the application context entirely","Injects a mock without replacing the real bean"]'::jsonb,
   '["Replaces a bean in the Spring application context with a Mockito mock"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does Mockito''s verify() method check?',
   '["That a method on a mock was called with specific arguments a specific number of times","That the return value of a method call is correct","That no exceptions were thrown by a method","That the mock object was properly initialised"]'::jsonb,
   '["That a method on a mock was called with specific arguments a specific number of times"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the purpose of the @SpringBootTest annotation?',
   '["Loads the full Spring application context for integration testing","Loads only the web layer for testing controllers","Creates a minimal context with only one bean","Runs tests without a Spring context"]'::jsonb,
   '["Loads the full Spring application context for integration testing"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which JUnit 5 annotation marks a method to run before each test method in a class?',
   '["@BeforeEach","@BeforeAll","@Before","@Setup"]'::jsonb,
   '["@BeforeEach"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

-- === Cloud & Infrastructure (4) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the difference between IaaS and PaaS cloud service models?',
   '["IaaS provides raw compute and storage; PaaS adds a managed runtime and middleware on top","IaaS is for public clouds; PaaS is for private clouds","IaaS is pay-per-request; PaaS is subscription-only","They are the same — the terms are interchangeable"]'::jsonb,
   '["IaaS provides raw compute and storage; PaaS adds a managed runtime and middleware on top"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does a Kubernetes Deployment resource manage?',
   '["The desired state, rolling update strategy, and replica count for a set of Pods","Persistent storage volumes for stateful workloads","Routing of external traffic into the cluster","Secrets and environment configuration for containers"]'::jsonb,
   '["The desired state, rolling update strategy, and replica count for a set of Pods"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the purpose of a Kubernetes liveness probe?',
   '["To detect when a container is unhealthy and restart it","To delay traffic to a container until it is ready to serve requests","To monitor CPU and memory usage of a container","To enforce resource quotas on a container"]'::jsonb,
   '["To detect when a container is unhealthy and restart it"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'In a blue-green deployment strategy, what is the purpose of the ''green'' environment?',
   '["It is the new version receiving no traffic initially; traffic is switched to it once validated","It is the production environment actively serving user traffic","It is a permanent staging environment for QA","It is a read-only replica for analytics queries"]'::jsonb,
   '["It is the new version receiving no traffic initially; traffic is switched to it once validated"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

-- === Data Structures & Algorithms (4) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the worst-case time complexity of QuickSort?',
   '["O(n²) — occurs when the pivot is consistently the smallest or largest element","O(n log n) — QuickSort is always linearithmic","O(n) — linear when the array is already sorted","O(log n) — due to the divide-and-conquer approach"]'::jsonb,
   '["O(n²) — occurs when the pivot is consistently the smallest or largest element"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What data structure is used internally to implement Dijkstra''s shortest-path algorithm efficiently?',
   '["Min-heap (priority queue)","Stack","Hash table","Balanced BST"]'::jsonb,
   '["Min-heap (priority queue)"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the space complexity of a recursive binary search implementation?',
   '["O(log n) — due to the call stack depth","O(1) — no additional space is used","O(n) — proportional to input size","O(n log n) — due to merging sub-problems"]'::jsonb,
   '["O(log n) — due to the call stack depth"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Which data structure guarantees O(log n) insertion, deletion, and lookup in all cases?',
   '["Balanced BST (e.g. Red-Black tree)","Hash table","Sorted array","Linked list"]'::jsonb,
   '["Balanced BST (e.g. Red-Black tree)"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

-- === Agile & Best Practices (3) ===

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the primary purpose of a Sprint Retrospective in Scrum?',
   '["To inspect the team''s process and identify improvements for the next sprint","To demonstrate the product increment to stakeholders","To plan the work for the upcoming sprint","To review and update the product backlog"]'::jsonb,
   '["To inspect the team''s process and identify improvements for the next sprint"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What does the Definition of Done (DoD) represent in Scrum?',
   '["A shared checklist of quality criteria a story must meet before it counts as complete","The acceptance criteria written by the Product Owner for a single story","The sprint goal agreed by the team at planning","The deployment runbook for releasing a feature"]'::jsonb,
   '["A shared checklist of quality criteria a story must meet before it counts as complete"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

WITH q AS (INSERT INTO mcq_question (id, question, options, correct_answers, created_at, updated_at) VALUES
  (gen_random_uuid(), 'What is the key difference between Scrum and Kanban?',
   '["Scrum uses fixed-length sprints and roles; Kanban uses a continuous flow with WIP limits","Scrum has no ceremonies; Kanban has a full set of rituals","Kanban requires story point estimation; Scrum does not","Scrum is for software teams only; Kanban is process-agnostic"]'::jsonb,
   '["Scrum uses fixed-length sprints and roles; Kanban uses a continuous flow with WIP limits"]'::jsonb, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

-- ============================================================
-- 7. Additional MCQ_PLUS — new banks (banks 7-11)
-- ============================================================

-- === Angular & Frontend (2) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Which RxJS operator should you use when you want to execute parallel HTTP requests and wait for all of them to complete?',
   '["forkJoin","combineLatest","zip","merge"]'::jsonb,
   '["forkJoin"]'::jsonb,
   'Explain why forkJoin is unsuitable when one of the source observables might never complete, and describe an alternative approach.',
   '["forkJoin","complete","never-ending observable","combineLatest","take(1)","stream","hot observable","cold observable","completion"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does the Angular signal input() API replace compared to the legacy @Input() decorator?',
   '["@Input() decorator — inputs become reactive signals automatically","@Output() decorator","@ViewChild() decorator","The ngOnChanges lifecycle hook only"]'::jsonb,
   '["@Input() decorator — inputs become reactive signals automatically"]'::jsonb,
   'Describe two concrete benefits that signal inputs provide over @Input() when used with OnPush change detection.',
   '["signal","reactive","OnPush","computed","effect","automatic tracking","zone.js","change detection","input()","@Input()"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

-- === TypeScript (2) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the TypeScript utility type Partial<T>?',
   '["A type identical to T but with all properties made optional","A type with all properties of T made required","A type with all properties of T made readonly","A subset of T containing only non-nullable properties"]'::jsonb,
   '["A type identical to T but with all properties made optional"]'::jsonb,
   'Describe a practical scenario where Partial<T> is the correct utility type, and explain what problem it solves compared to creating a separate interface.',
   '["Partial","optional","update DTO","patch","Required","Record","Readonly","utility type","mapped type","TypeScript"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does the infer keyword do inside a conditional type in TypeScript?',
   '["Introduces a type variable that is inferred from the matched type at the point of use","Asserts that a type is non-null","Narrows a union type to a single member","Creates a recursive generic constraint"]'::jsonb,
   '["Introduces a type variable that is inferred from the matched type at the point of use"]'::jsonb,
   'Write and explain a conditional type that extracts the return type of a function type using infer (similar to ReturnType<T>).',
   '["infer","conditional type","ReturnType","extends","extract","inferred type variable","mapped type","generic","TypeScript","T extends (...args: any[]) => infer R"]'::jsonb,
   5, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

-- === Testing & Quality (2) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is the primary advantage of Testcontainers over an in-memory database (H2) for integration tests?',
   '["Tests run against a real database engine, catching dialect-specific bugs H2 silently accepts","Testcontainers is faster than H2","H2 has no SQL support","Testcontainers requires no Docker installation"]'::jsonb,
   '["Tests run against a real database engine, catching dialect-specific bugs H2 silently accepts"]'::jsonb,
   'Describe a specific class of SQL or Hibernate behaviour that H2 compatibility mode may incorrectly emulate, and explain how Testcontainers eliminates that risk.',
   '["dialect","PostgreSQL-specific","JSONB","window function","H2 compatibility","schema validation","Liquibase","migration","real container","docker"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What does code coverage percentage actually measure?',
   '["The proportion of source lines or branches exercised by the test suite","The proportion of requirements that are tested","The correctness of the test assertions","The number of bugs found per line of code"]'::jsonb,
   '["The proportion of source lines or branches exercised by the test suite"]'::jsonb,
   'Explain why 100% line coverage does not guarantee a bug-free codebase. Give a concrete example of a defect that high coverage would miss.',
   '["line coverage","branch coverage","mutation testing","assertion","false sense of security","path coverage","edge case","test quality","coverage gap","incorrect assertion"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

-- === Data Structures & Algorithms (2) ===

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is dynamic programming?',
   '["An optimisation technique that solves problems by breaking them into overlapping sub-problems and caching solutions","A programming paradigm using dynamic dispatch","A runtime code generation technique","A technique for solving graph problems with BFS"]'::jsonb,
   '["An optimisation technique that solves problems by breaking them into overlapping sub-problems and caching solutions"]'::jsonb,
   'Describe the difference between top-down memoisation and bottom-up tabulation, using Fibonacci as a concrete example.',
   '["memoisation","tabulation","top-down","bottom-up","sub-problem","overlapping","Fibonacci","recursion","cache","space complexity"]'::jsonb,
   5, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

WITH q AS (INSERT INTO mcq_plus_question (id, question, options, correct_answers, follow_up_question, follow_up_keywords, follow_up_marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What distinguishes DFS from BFS in terms of the underlying data structure used?',
   '["DFS uses a stack (or recursion); BFS uses a queue","DFS uses a queue; BFS uses a stack","Both use a priority queue","DFS uses a hash set; BFS uses a sorted array"]'::jsonb,
   '["DFS uses a stack (or recursion); BFS uses a queue"]'::jsonb,
   'Give a concrete use case where BFS is preferred over DFS and explain why the underlying data structure makes BFS correct for that problem.',
   '["BFS","DFS","shortest path","level order","queue","FIFO","unweighted graph","cycle detection","connected components","visited set"]'::jsonb,
   4, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

-- ============================================================
-- 8. Additional TEXT — new banks (banks 7-12)
-- ============================================================

-- === Angular & Frontend (3) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the Angular component lifecycle. List at least five lifecycle hooks in the order they fire and describe when each is appropriate to use.',
   '["ngOnChanges","ngOnInit","ngDoCheck","ngAfterContentInit","ngAfterViewInit","ngOnDestroy","lifecycle","constructor","input","change detection"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Compare reactive forms and template-driven forms in Angular. When would you choose one over the other, and what are the trade-offs?',
   '["reactive forms","template-driven","FormGroup","FormControl","NgModel","testability","dynamic validation","Validators","synchronous","model-driven"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe how you would implement a global HTTP error interceptor in Angular that shows a toast notification for 4xx and 5xx responses.',
   '["HttpInterceptor","intercept","catchError","HttpErrorResponse","status code","inject","toast","RxJS","throwError","retry"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

-- === TypeScript (3) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the structural type system in TypeScript. How does it differ from nominal typing, and what practical implications does it have for interface compatibility?',
   '["structural typing","nominal typing","duck typing","assignability","interface","compatible","shape","property","Java","class"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What are TypeScript decorators? Describe how a method decorator works and give a practical example.',
   '["decorator","metadata","reflect-metadata","method decorator","class decorator","parameter decorator","AOP","Angular","experimental","target"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain how TypeScript''s type narrowing works. Give three examples of narrowing techniques and describe what type guard each one uses.',
   '["typeof","instanceof","in operator","discriminated union","truthiness","type predicate","is keyword","narrowing","control flow","assertion function"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM q;

-- === Testing & Quality (3) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the testing pyramid. What proportion of unit, integration, and end-to-end tests does it recommend and why?',
   '["testing pyramid","unit test","integration test","end-to-end","fast feedback","cost","maintenance","flaky","coverage","isolation"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the difference between stub, mock, and spy in unit testing. Provide a concrete example of each using Mockito.',
   '["stub","mock","spy","Mockito","verify","when","thenReturn","doReturn","partial mock","test double"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'How would you write a comprehensive integration test for a Spring Boot REST endpoint that creates a resource? Describe setup, execution, and assertions.',
   '["@SpringBootTest","MockMvc","Testcontainers","@Transactional","@AutoConfigureMockMvc","perform","andExpect","status","jsonPath","database assertion"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

-- === Cloud & Infrastructure (3) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe a complete CI/CD pipeline for a Spring Boot microservice from code commit to production deployment. Include the key stages and tools.',
   '["CI/CD","pipeline","build","test","Docker","push","registry","deploy","rollback","approval gate","artifact","GitOps","Kubernetes"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is infrastructure as code? Compare Terraform and Ansible in terms of purpose, approach, and when you would choose each.',
   '["IaC","Terraform","Ansible","declarative","imperative","idempotent","state file","provider","playbook","provisioning","configuration management"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain the twelve-factor app methodology. Choose four factors and describe how you would apply each to a Spring Boot application.',
   '["twelve-factor","config","codebase","dependencies","processes","port binding","disposability","dev/prod parity","logs","backing services","environment variables"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

-- === Data Structures & Algorithms (3) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain Big-O notation. Describe O(1), O(log n), O(n), O(n log n), and O(n²) with a concrete algorithm example for each.',
   '["Big-O","O(1)","O(log n)","O(n)","O(n log n)","O(n²)","time complexity","space complexity","growth rate","algorithm analysis"]'::jsonb,
   8, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Compare a hash table and a balanced BST for storing a sorted set of integers. Describe time complexities for insert, lookup, and range query.',
   '["hash table","BST","Red-Black tree","O(1)","O(log n)","range query","ordered","hash collision","sorted order","TreeMap","HashMap"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Describe the sliding window technique for solving subarray problems. Explain the pattern and give a concrete example problem it solves efficiently.',
   '["sliding window","two pointers","subarray","fixed window","variable window","O(n)","brute force","contiguous","maximum sum","minimum length"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000011' FROM q;

-- === Agile & Best Practices (3) ===

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is technical debt? Describe the different types and how you would communicate its impact to non-technical stakeholders.',
   '["technical debt","deliberate","inadvertent","reckless","prudent","refactoring","maintenance cost","velocity","interest","backlog","quadrant"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Explain continuous improvement in an Agile team. Describe at least three practices or ceremonies that support it.',
   '["retrospective","kaizen","blameless","inspect and adapt","metrics","cycle time","velocity","improvement","action item","psychological safety"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

WITH q AS (INSERT INTO text_question (id, question, keywords, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'What is a well-written user story? Describe the INVEST criteria and the ''As a / I want / So that'' format with a concrete example.',
   '["INVEST","Independent","Negotiable","Valuable","Estimable","Small","Testable","user story","As a","acceptance criteria","story splitting"]'::jsonb,
   6, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

-- ============================================================
-- 9. DOC Questions (6)
-- NOTE: doc_question.id has no DB default — must supply gen_random_uuid() explicitly.
-- ============================================================

WITH q AS (INSERT INTO doc_question (id, question, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Upload a UML class diagram showing the relationships between the core domain entities in your most recent Java project. Include at least four classes with their key attributes and associations.',
   15, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000001' FROM q;

WITH q AS (INSERT INTO doc_question (id, question, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Upload a document describing the REST API design for a simple order management system. Include at least five endpoints with HTTP methods, request/response bodies, and appropriate status codes.',
   15, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000004' FROM q;

WITH q AS (INSERT INTO doc_question (id, question, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Upload a wireframe or annotated screenshot of an Angular component you have built. Explain the component''s responsibilities, inputs, outputs, and any notable implementation decisions.',
   12, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM q;

WITH q AS (INSERT INTO doc_question (id, question, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Upload a completed test plan document for a feature of your choice. Include scope, test types, test cases, and acceptance criteria.',
   15, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM q;

WITH q AS (INSERT INTO doc_question (id, question, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Upload a CI/CD pipeline diagram for a containerised Spring Boot application. Label each stage and describe the tools used.',
   12, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM q;

WITH q AS (INSERT INTO doc_question (id, question, marks, created_at, updated_at) VALUES
  (gen_random_uuid(),
   'Upload a completed sprint retrospective document from a recent project, or a template you would use. Annotate each section explaining its purpose.',
   10, NOW(), NOW()) RETURNING id)
INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM q;

-- ============================================================
-- 10. GROUP Questions (5)
-- Multi-CTE pattern: gq inserts the group and returns its id,
-- bk wires the bank association, final INSERT adds children.
-- ============================================================

-- === Angular & Frontend ===
WITH gq AS (
  INSERT INTO group_question (id, question, ordered, created_at, updated_at) VALUES
    (gen_random_uuid(),
     'Angular Lifecycle Hooks — answer each child question about the Angular component lifecycle.',
     true, NOW(), NOW()) RETURNING id
),
bk AS (
  INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000007' FROM gq
)
INSERT INTO group_question_child (id, group_id, question_text, keywords, marks, display_order, created_at, updated_at)
SELECT gen_random_uuid(), gq.id,
       'What is the difference between the constructor and ngOnInit? When is each appropriate for initialisation logic?',
       '["constructor","ngOnInit","dependency injection","Angular","lifecycle","initialisation","input","async"]'::jsonb,
       4, 1, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'When does ngOnChanges fire, and what does the SimpleChanges parameter contain?',
       '["ngOnChanges","SimpleChanges","previousValue","currentValue","firstChange","@Input","reference change","OnPush"]'::jsonb,
       4, 2, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'What cleanup should always be performed in ngOnDestroy, and why is it critical for memory management?',
       '["ngOnDestroy","unsubscribe","takeUntilDestroyed","memory leak","EventEmitter","timer","clearInterval","subscription"]'::jsonb,
       4, 3, NOW(), NOW() FROM gq;

-- === TypeScript ===
WITH gq AS (
  INSERT INTO group_question (id, question, ordered, created_at, updated_at) VALUES
    (gen_random_uuid(),
     'TypeScript Type System — answer each question about TypeScript''s type constructs.',
     true, NOW(), NOW()) RETURNING id
),
bk AS (
  INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000008' FROM gq
)
INSERT INTO group_question_child (id, group_id, question_text, keywords, marks, display_order, created_at, updated_at)
SELECT gen_random_uuid(), gq.id,
       'What is the difference between interface and type alias in TypeScript? Give one example where each is preferred.',
       '["interface","type alias","extends","intersection","union","declaration merging","object shape","primitive","TypeScript"]'::jsonb,
       4, 1, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'Explain what a generic constraint (e.g. T extends SomeType) achieves and why it is needed.',
       '["generic","constraint","extends","T extends","keyof","type safety","compile error","flexibility","reusable","narrowing"]'::jsonb,
       4, 2, NOW(), NOW() FROM gq;

-- === Testing & Quality ===
WITH gq AS (
  INSERT INTO group_question (id, question, ordered, created_at, updated_at) VALUES
    (gen_random_uuid(),
     'Unit Testing Fundamentals — answer each question about unit testing best practices.',
     false, NOW(), NOW()) RETURNING id
),
bk AS (
  INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000009' FROM gq
)
INSERT INTO group_question_child (id, group_id, question_text, keywords, marks, display_order, created_at, updated_at)
SELECT gen_random_uuid(), gq.id,
       'What is the Arrange-Act-Assert (AAA) pattern? Describe each phase with a concrete JUnit 5 example.',
       '["AAA","Arrange","Act","Assert","JUnit 5","@Test","given-when-then","test structure","setup","assertion"]'::jsonb,
       4, 1, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'What makes a unit test brittle? List three common causes and describe how to avoid them.',
       '["brittle","implementation detail","over-specification","mock","tight coupling","test fragility","refactoring","behaviour","black box","verify"]'::jsonb,
       4, 2, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'When is it appropriate NOT to mock a dependency in a unit test? Give a concrete example.',
       '["value object","pure function","no I/O","in-memory","simple collaborator","real dependency","test double","mock","stub","unit boundary"]'::jsonb,
       3, 3, NOW(), NOW() FROM gq;

-- === Cloud & Infrastructure ===
WITH gq AS (
  INSERT INTO group_question (id, question, ordered, created_at, updated_at) VALUES
    (gen_random_uuid(),
     'Containerisation & Orchestration — answer each question about Docker and Kubernetes.',
     true, NOW(), NOW()) RETURNING id
),
bk AS (
  INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000010' FROM gq
)
INSERT INTO group_question_child (id, group_id, question_text, keywords, marks, display_order, created_at, updated_at)
SELECT gen_random_uuid(), gq.id,
       'What is a multi-stage Docker build and what problem does it solve for a Spring Boot application?',
       '["multi-stage","Dockerfile","build stage","runtime stage","image size","Maven","JDK","JRE","layer","artifact"]'::jsonb,
       5, 1, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'What is the difference between a Kubernetes Service of type ClusterIP, NodePort, and LoadBalancer?',
       '["ClusterIP","NodePort","LoadBalancer","internal","external","port","selector","cloud provider","ingress","routing"]'::jsonb,
       5, 2, NOW(), NOW() FROM gq;

-- === Agile & Best Practices ===
WITH gq AS (
  INSERT INTO group_question (id, question, ordered, created_at, updated_at) VALUES
    (gen_random_uuid(),
     'Scrum Ceremonies — answer each question about Scrum events and their purpose.',
     false, NOW(), NOW()) RETURNING id
),
bk AS (
  INSERT INTO question_question_bank SELECT id, 'bb000001-0000-0000-0000-000000000012' FROM gq
)
INSERT INTO group_question_child (id, group_id, question_text, keywords, marks, display_order, created_at, updated_at)
SELECT gen_random_uuid(), gq.id,
       'What is the purpose of sprint planning? Who attends and what are the two main outputs?',
       '["sprint planning","sprint goal","sprint backlog","Product Owner","Scrum Master","Dev team","capacity","velocity","output","commitment"]'::jsonb,
       4, 1, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'What is the daily standup for? Describe the three standard questions and a common anti-pattern to avoid.',
       '["daily standup","yesterday","today","blocker","anti-pattern","status report","problem solving","15 minutes","Scrum Master","impediment"]'::jsonb,
       4, 2, NOW(), NOW() FROM gq
UNION ALL
SELECT gen_random_uuid(), gq.id,
       'What is the difference between a sprint review and a sprint retrospective? Who should attend each?',
       '["sprint review","sprint retrospective","stakeholders","increment","feedback","process","inspect","adapt","Product Owner","team"]'::jsonb,
       4, 3, NOW(), NOW() FROM gq;
