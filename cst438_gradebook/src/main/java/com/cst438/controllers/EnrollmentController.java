package com.cst438.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Assignment;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentDTO;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.AssignmentListDTO.AssignmentDTO;

@RestController
public class EnrollmentController {

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	EnrollmentRepository enrollmentRepository;

	/*
	 * endpoint used by registration service to add an enrollment to an existing
	 * course.
	 */
	@PostMapping("/enrollment")
	@Transactional
	public EnrollmentDTO addEnrollment(@RequestBody EnrollmentDTO enrollmentDTO) {
		
		//TODO  complete this method in homework 4
		
		Enrollment enrollment = new Enrollment();
		
		//enrollment.setId(enrollmentDTO.id);
		enrollment.setStudentName(enrollmentDTO.studentName);
		enrollment.setStudentEmail(enrollmentDTO.studentEmail);
		Course course = courseRepository.findByCourse_id(enrollmentDTO.course_id);
		enrollment.setCourse(course);
		//enrollment.setAssignmentGrades(enrollment.getAssignmentGrades());
		
		Enrollment newEnrollment = enrollmentRepository.save(enrollment);
		
		return createEnrollmentDTO(newEnrollment);
		
		//return null;
		
	}
	
	private EnrollmentDTO createEnrollmentDTO(Enrollment e) {
	     EnrollmentDTO enrollmentDTO = new EnrollmentDTO(e.getStudentEmail().toString(), 
	    		 e.getStudentName(), e.getCourse().getCourse_id());

	     return enrollmentDTO;
	}

}
