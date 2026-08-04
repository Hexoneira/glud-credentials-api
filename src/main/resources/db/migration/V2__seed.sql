INSERT INTO tenants (name, tenant_code, director, member_limit, status)
VALUES ('GLUD', 'GLUD', 'Administración GLUD', 100, 'ACTIVE');

INSERT INTO users (username, password, codigo, tenant_id, rol)
VALUES ('superadmin', '$2a$10$2EwbAGQfR6s4wjVrMSBad.hCbJ/lFP9uubWymeVDWZvzZZsv1DAzK', '20210000000', 1, 'SUPER_ADMIN');
