package com.drivingschool.drivingsession;

public class InstructorSessionAccessDeniedException extends RuntimeException {

    public InstructorSessionAccessDeniedException() {
        super("Instructors can only access their own driving sessions");
    }
}
