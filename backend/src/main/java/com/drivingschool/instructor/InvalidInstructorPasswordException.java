package com.drivingschool.instructor;

public class InvalidInstructorPasswordException extends RuntimeException {

    public InvalidInstructorPasswordException() {
        super("The initial password must not exceed 72 UTF-8 bytes");
    }
}
