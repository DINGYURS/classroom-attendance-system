create table user
(
    user_id     bigint auto_increment comment '主键ID'
        primary key,
    username    varchar(64)                        not null comment '账号(学号/工号)',
    password    varchar(128)                       not null comment '加密密码',
    real_name   varchar(64)                        not null comment '真实姓名',
    role        tinyint  default 2                 not null comment '角色: 1-Teacher, 2-Student',
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint uk_username
        unique (username)
)
    comment '用户基础表';

create table student
(
    user_id        bigint       not null comment '关联user.user_id'
        primary key,
    student_number varchar(32)  not null comment '学号',
    admin_class    varchar(64)  not null comment '行政班级(如: 计科225)',
    gender         tinyint      null comment '性别: 1-男, 2-女',
    avatar_object_key varchar(255) null comment '学生头像/人脸底库图对象键(MinIO)',
    feature_vector text         null comment '人脸特征值(AES加密密文)',
    constraint fk_student_user
        foreign key (user_id) references user (user_id)
            on delete cascade
)
    comment '学生信息表';

create table teacher
(
    user_id    bigint      not null comment '关联user.user_id'
        primary key,
    job_number varchar(32) not null comment '工号',
    constraint fk_teacher_user
        foreign key (user_id) references user (user_id)
            on delete cascade
)
    comment '教师信息表';

create table course
(
    course_id   bigint auto_increment
        primary key,
    course_name varchar(128)                       not null comment '课程名称',
    teacher_id  bigint                             not null comment '所属教师ID(关联teacher.user_id)',
    semester    varchar(32)                        null comment '学期(如: 2025-2026-1)',
    description varchar(512)                       null comment '课程描述',
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint fk_course_teacher
        foreign key (teacher_id) references teacher (user_id)
)
    comment '课程表';

create table attendance_session
(
    session_id     bigint auto_increment
        primary key,
    course_id      bigint                             not null comment '所属课程',
    image_object_keys json                            not null comment '点名合照的 MinIO 对象键列表(JSON Array)',
    total_student  int      default 0                 not null comment '应到人数',
    actual_student int      default 0                 not null comment '实到人数',
    start_time     datetime default CURRENT_TIMESTAMP null comment '点名时间',
    constraint fk_session_course
        foreign key (course_id) references course (course_id)
)
    comment '考勤会话表';

create table attendance_record
(
    record_id        bigint auto_increment
        primary key,
    session_id       bigint            not null comment '关联会话',
    student_id       bigint            not null comment '关联学生',
    status           tinyint default 0 not null comment '考勤状态: 0-缺勤, 1-已到, 2-迟到, 3-请假',
    similarity_score decimal(5, 4)     null comment '识别相似度(如 0.8521)',
    face_location    varchar(64)       null comment '人脸坐标(如 [x,y,w,h])',
    update_type      tinyint default 1 null comment '修改类型: 1-算法自动, 2-人工修正',
    constraint uk_session_student
        unique (session_id, student_id),
    constraint fk_record_session
        foreign key (session_id) references attendance_session (session_id)
            on delete cascade,
    constraint fk_record_student
        foreign key (student_id) references student (user_id)
)
    comment '考勤明细表';

create table attendance_detection
(
    detection_id     bigint auto_increment
        primary key,
    session_id       bigint            not null comment '关联会话',
    image_index      int               not null comment '来源图片索引',
    face_index       int               not null comment '图片内人脸序号',
    bbox             varchar(128)      not null comment '检测框坐标 JSON，如 [x1,y1,x2,y2]',
    detection_score  decimal(6, 4)     null comment '检测置信度',
    matched          tinyint default 0 not null comment '是否匹配成功: 0-未匹配, 1-已匹配',
    student_id       bigint            null comment '匹配到的学生 ID',
    record_id        bigint            null comment '匹配到的考勤记录 ID',
    similarity_score decimal(5, 4)     null comment '匹配相似度',
    ignored          tinyint default 0 not null comment '是否已忽略: 0-否, 1-是',
    ignore_reason    varchar(128)      null comment '忽略原因',
    create_time      datetime default CURRENT_TIMESTAMP null,
    constraint fk_detection_session
        foreign key (session_id) references attendance_session (session_id)
            on delete cascade
)
    comment '考勤检测结果表';

create index idx_course
    on attendance_session (course_id);

create index idx_detection_session
    on attendance_detection (session_id);

create index idx_detection_session_image
    on attendance_detection (session_id, image_index);

create index idx_teacher
    on course (teacher_id);

create table course_student
(
    id         bigint auto_increment
        primary key,
    course_id  bigint                             not null,
    student_id bigint                             not null comment '关联student.user_id',
    join_time  datetime default CURRENT_TIMESTAMP null,
    constraint uk_course_student
        unique (course_id, student_id),
    constraint fk_cs_course
        foreign key (course_id) references course (course_id)
            on delete cascade,
    constraint fk_cs_student
        foreign key (student_id) references student (user_id)
            on delete cascade
)
    comment '课程学生关联表';

create table attendance_notice
(
    notice_id       bigint auto_increment
        primary key,
    teacher_id      bigint                             not null comment '发送教师ID',
    student_id      bigint                             not null comment '接收学生ID',
    course_id       bigint                             null comment '相关课程ID，为空表示综合提醒',
    notice_type     tinyint  default 1                 not null comment '通知类型: 1-缺勤提醒',
    absent_count    int      default 0                 not null comment '发送时累计缺勤次数快照',
    title           varchar(128)                       not null comment '通知标题',
    content         varchar(512)                       not null comment '通知内容',
    send_status     tinyint  default 1                 not null comment '发送状态: 0-待发送, 1-已发送, 2-发送失败',
    read_status     tinyint  default 0                 not null comment '阅读状态: 0-未读, 1-已读',
    send_time       datetime default CURRENT_TIMESTAMP null comment '发送时间',
    read_time       datetime                           null comment '阅读时间',
    create_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint fk_notice_teacher
        foreign key (teacher_id) references teacher (user_id)
            on delete cascade,
    constraint fk_notice_student
        foreign key (student_id) references student (user_id)
            on delete cascade,
    constraint fk_notice_course
        foreign key (course_id) references course (course_id)
            on delete set null
)
    comment '考勤提醒通知记录表';

create index idx_session_course_time
    on attendance_session (course_id, start_time);

create index idx_record_student_status
    on attendance_record (student_id, status);

create index idx_record_student_session
    on attendance_record (student_id, session_id);

create index idx_cs_student
    on course_student (student_id);

create index idx_notice_teacher_send_time
    on attendance_notice (teacher_id, send_time);

create index idx_notice_student_read
    on attendance_notice (student_id, read_status, send_time);

create index idx_notice_course
    on attendance_notice (course_id, send_time);

