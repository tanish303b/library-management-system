-- Optional sample books. The application offers to load these on first run.

INSERT IGNORE INTO books (isbn, title, author, category, total_copies, available_copies) VALUES
    ('9780134685991', 'Effective Java', 'Joshua Bloch', 'Programming', 3, 3),
    ('9780596009205', 'Head First Java', 'Kathy Sierra', 'Programming', 4, 4),
    ('9780262033848', 'Introduction to Algorithms', 'Thomas H. Cormen', 'Algorithms', 2, 2),
    ('9780132350884', 'Clean Code', 'Robert C. Martin', 'Programming', 2, 2),
    ('9780073523323', 'Database System Concepts', 'Abraham Silberschatz', 'Databases', 3, 3),
    ('9781118063330', 'Operating System Concepts', 'Abraham Silberschatz', 'Operating Systems', 2, 2),
    ('9780132126953', 'Computer Networks', 'Andrew S. Tanenbaum', 'Networking', 2, 2),
    ('9788173711466', 'Wings of Fire', 'A. P. J. Abdul Kalam', 'Biography', 5, 5)
