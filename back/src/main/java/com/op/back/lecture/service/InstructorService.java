package com.op.back.lecture.service;


import com.op.back.lecture.dto.InstructorCreateRequest;
import com.op.back.lecture.model.Instructor;


public interface InstructorService {
    
    public void registerInstructor(InstructorCreateRequest req,String currentUid);
    public Instructor getInstructor(String instructorUid);
}
