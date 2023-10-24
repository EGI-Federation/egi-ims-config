insert into sys.users (checkinuserid, fullname, email)
values ('e9c37aa0d1cf14c56e560f9f9915da6761f54383badb501a2867bc43581b835c@egi.eu', 'Levente Farkas', 'levente.farkas@egi.eu');

insert into sys.roles (role, name, version, status, changedon, changedescription, assignable, tasks)
values ('process-staff', 'Process Staff', 1, 1, '2021-02-19T19:23:18', 'First version', false,
'TBD'),

       ('process-owner', 'Process Owner', 1, 1, '2021-02-19T19:23:18', 'First version', false,
'Act as the primary contact point for concerns in the context of governing one specific IMS process.
- Define and approve goals and policies in the context of the process according to the overall IMS goals and policies
- Nominate the process manager, and ensure he/she is competent to fulfill this role
- Approve changes/improvements to the operational process, such as (significant) changes to the process definition
- Decide on the provision of resources dedicated to the process and its activities
- Based on process monitoring and reviews, decide on necessary changes in the process-specific goals, policies and provided resources'),

       ('process-manager', 'Process Manager', 1, 1, '2021-02-19T19:23:18', 'First version', false,
'Act as the primary contact point for operational concerns in the context of the process.
- Maintain the process definition/description and ensure it is available to relevant persons
- Maintain an adequate level of awareness and competence of the people involved in the process
- Monitor and keep track of the process execution and results (incl. process reviews)
- Report on process performance to the process owner
- Escalate to the process owner, if necessary
- Identify opportunities for improving the effectiveness and efficiency of the process'),

       ('report-owner', 'Report Owner', 1, 1, '2021-02-19T19:23:18', 'First version', false,
'TBD'),

       ('service-owner', 'Service Owner', 1, 1, '2021-02-19T19:23:18', 'First version', false,
'Overall responsibility for one specific service which is part of the service portfolio.
- Act as the primary contact point for all (process-independent) concerns in the context of that specific service
- Act as an “expert” for the service in technical and non-technical concerns
- Maintain the core service documentation, such as the service specification/description
- Be kept informed of every event, situation or change connected to the service
- Be involved in tasks significantly related to the service as part of selected IMS processes, in particular, SPM and SLM (see: process-specific role models)
- Report on the service to the IMS owner
- Define and maintain individual service roadmap with annual objectives for a 3-year period, a summary of resources needed (including financial), usage and satisfaction statistics'),

       ('ims-owner', 'IMS Owner', 1, 1, '2021-02-19T19:23:18', 'First version', true,
'Senior accountable owner of the entire Integrated Management System (IMS).
- Overall accountability for all IMS-related activities
- Act as the primary contact point for concerns in the context of governing the entire IMS
- Define and approve goals and policies for the entire IMS
- Nominate the process owners and/or managers, and ensure they are competent to fulfill their roles
- Approve the first definition of processes/procedures
- Approve changes to the overall IMS
- Decide on the provision of resources dedicated to IMS
- Based on monitoring and reviews, decide on necessary changes in the goals, policies and provide resources for the IMS
- Appoint and approve service owners'),

       ('ims-manager', 'IMS Manager', 1, 1, '2021-02-19T19:23:18', 'First version', true,
'Act as the primary contact point for all tactical concerns (including planning and development) in the context of the entire IMS.
- Maintain the service management plan and ensure it is available to relevant stakeholders
- Ensure service management processes are implemented according to approved goals and policies
- Maintain an adequate level of awareness and competence of the people involved in the IMS, in particular, the process managers
- Monitor and keep track of the suitability, effectiveness and maturity of the entire IMS
- Report and, if necessary, escalate to the IMS owner
- Identify opportunities for improving the effectiveness and efficiency of the IMS'),

       ('ims-developer', 'IMS Developer', 1, 0, '2023-09-02T19:23:18', 'First version', true,
'- Make the necessary software changes to Management System API so that requested changes to the management system,
governance body, budget, policy, task, workshop, procedure, KPI, report, and role entities are implemented
- Improve the IMS front-end to allow exploiting all features of the Management System API'),

       ('strategy-coordinator', 'Strategy Coordinator', 1, 1, '2021-02-19T19:23:18', 'First version', true,
'Is part of the IMS coordination team responsible for strategic decisions related to the IMS, chaired by the IMS owner.
- Support the IMS owner at the strategic level of IMS
- Maintain a formal communication channel between the IMS owner and the IMS manager'),

       ('operations-coordinator', 'Operations Coordinator', 1, 1, '2021-02-19T19:23:18', 'First version', true,
'Is part of the team responsible for overall monitoring of the IMS, chaired by the IMS manager.
- Support the IMS owner at the operations level of IMS
- Maintain a formal communication channel between the CSI process and the IMS manager');

insert into sys.role_editor (role_id, user_id)
values (1, 1),
       (2, 1),
       (3, 1),
       (4, 1),
       (5, 1),
       (6, 1),
       (7, 1),
       (8, 1),
       (9, 1),
       (10, 1);

insert into sys.processes (status, reviewfrequency, frequencyunit, nextreview, changedon, changedescription, contact, goals, scope)
VALUES (0, 1, 'year', '2024-05-14', '2021-02-19T19:23:18', 'First draft', null,
        'The primary purpose of this process is...',
        'The scope of this process is...');

insert into sys.process_editor (process_id, user_id)
values (1, 1);
