# Roadmap

## v1 — Foundation *(current)*

The initial release establishes the core platform with a working end-to-end assessment flow.

### Candidate Management
- [ ] Admin/Marker registers candidates (creates user account + candidate profile)
- [ ] Welcome email sent with login credentials on registration

### Question Bank
- [ ] Create and categorise question banks
- [ ] Add MCQ questions (single and multiple correct answers)
- [ ] Add text-based questions (freeform, short or long answer)
- [ ] Add document / code submission questions (max 1 per assessment)
- [ ] Group questions into standard or structured question groups
- [ ] Reuse questions across multiple assessments

### Assessment Generation
- [ ] Select questions from a bank using topic and type rules
- [ ] Automatically exclude questions previously seen by the candidate
- [ ] Enforce doc question limit (at most one per assessment)
- [ ] Create assessment with a signed invitation token
- [ ] Send invitation email with a secure access link

### Assessment Flow
- [ ] Candidate accesses assessment via token-gated link
- [ ] Server records start time; status transitions to `IN_PROGRESS`
- [ ] Frontend countdown timer driven by server-reported time limit
- [ ] Candidate saves progress incrementally
- [ ] Server-side auto-submit when time limit expires
- [ ] Candidate explicit submit; status transitions to `SUBMITTED`

### Marking & Results
- [ ] Auto-mark MCQ responses on submission
- [ ] Marker review queue shows pending submissions
- [ ] Marker awards marks per text and document response
- [ ] Marker finalises total marks; status transitions to `MARKED`
- [ ] Result notification email sent to candidate
- [ ] Candidate views own results

### Auth & Security
- [ ] JWT-based stateless authentication
- [ ] Role-based access control (Admin, Marker, Candidate)
- [ ] BCrypt password hashing (strength 12)
- [ ] Separate short-lived invitation tokens for assessment access

---

## v2 — Automated Testing *(planned)*

> These features should influence v1 architecture decisions but are not required for the initial release.

- [ ] Sandboxed code execution environment
- [ ] Support Java, Python, and C# submissions
- [ ] Predefined test cases per coding question
- [ ] Automatic grading of coding questions based on test results
- [ ] Resource limits on execution (time, memory)
- [ ] Test result reporting in the submission review UI

---

## v3 — Integrity & Intelligence *(planned)*

- [ ] Tab-switching and inactivity monitoring during assessments
- [ ] Timing anomaly detection (unusually fast completions)
- [ ] Text similarity detection across submissions
- [ ] AI-generated content heuristics
- [ ] Reported issues linked to submissions (`reported_issues` table)
- [ ] Assessor review workflow for flagged submissions

---

## Future Considerations

- OAuth2 / SSO integration (Google, Microsoft)
- Candidate self-registration with admin approval flow
- Assessment templates and cloning
- Bulk candidate import (CSV)
- Analytics dashboard (completion rates, score distributions, question difficulty)
- Multi-tenant / company isolation
