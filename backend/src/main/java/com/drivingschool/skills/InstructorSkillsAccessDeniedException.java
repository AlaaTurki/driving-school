package com.drivingschool.skills;

public class InstructorSkillsAccessDeniedException extends RuntimeException {

    public InstructorSkillsAccessDeniedException() {
        super("Instructor can only access skills for their own candidates");
    }
}
