package com.cst438.controllers;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentListDTO;
import com.cst438.domain.AssignmentListDTO.AssignmentDTO;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseDTOG;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.GradebookDTO;
import com.cst438.services.RegistrationService;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class GradeBookController {
	
	@Autowired
	AssignmentRepository assignmentRepository;
	
	@Autowired
	AssignmentGradeRepository assignmentGradeRepository;
	
	@Autowired
	CourseRepository courseRepository;
	
	@Autowired
	RegistrationService registrationService;
	
	// get assignments for an instructor that need grading
	@GetMapping("/gradebook")
	public AssignmentListDTO getAssignmentsNeedGrading( ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		List<Assignment> assignments = assignmentRepository.findNeedGradingByEmail(email);
		AssignmentListDTO result = new AssignmentListDTO();
		for (Assignment a: assignments) {
			result.assignments.add(new AssignmentListDTO.AssignmentDTO(a.getId(), a.getCourse().getCourse_id(), a.getName(), a.getDueDate().toString() , a.getCourse().getTitle()));
		}
		return result;
	}
	
	// As an instructor for a course , I can add a new assignment for my course.  
	// The assignment has a name and a due date.
	@PostMapping("/gradebook/add")
	@Transactional
	public AssignmentDTO addAssignment(@RequestBody AssignmentDTO assignmentDTO) {
		Assignment duplicateAssignment = assignmentRepository.findById(assignmentDTO.assignmentId);
		
		if(duplicateAssignment == null) {
			Assignment assignment = new Assignment();
			assignment.setName(assignmentDTO.assignmentName);
			
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
			Date dueDate = null;
			try {
				dueDate = (Date) formatter.parse(assignmentDTO.dueDate);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			assignment.setDueDate(dueDate);
			
			Course course = courseRepository.findByCourse_id(assignmentDTO.courseId);
			assignment.setCourse(course);
			
			//assignment.setNeedsGrading(1);
			Assignment newAssignment = assignmentRepository.save(assignment);
			
			return createAssignmentDTO(newAssignment);
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment already exists.");
		}
		
	}
	
	private AssignmentDTO createAssignmentDTO(Assignment a) {
        AssignmentDTO assignmentDTO = new AssignmentDTO(a.getId(), assignmentRepository.findById(a.getId()).getId(), a.getName(),
        		a.getName(), a.getDueDate().toString());

        return assignmentDTO;
    }
	
	@RequestMapping(path="/gradebook/{assignmentId}", method = RequestMethod.DELETE)
	@Transactional
	public void dropAssignment( @PathVariable int assignmentId) {
		
		String email = "dwisneski@csumb.edu";
		Assignment a = checkAssignment(assignmentId, email);
		
		Assignment assignment = assignmentRepository.findById(assignmentId);
		
		//verify there are no grades
		if (assignment.getNeedsGrading() == 1) {
			assignmentRepository.delete(assignment);
		} else {
			throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "Can't Delete - Assignment has grades");
		}
	}
	
	@GetMapping("/gradebook/{id}")
	public GradebookDTO getGradebook(@PathVariable("id") Integer assignmentId  ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		Assignment assignment = checkAssignment(assignmentId, email);
		
		// get the enrollment for the course
		//  for each student, get the current grade for assignment, 
		//   if the student does not have a current grade, create an empty grade
		GradebookDTO gradebook = new GradebookDTO();
		gradebook.assignmentId= assignmentId;
		gradebook.assignmentName = assignment.getName();
		for (Enrollment e : assignment.getCourse().getEnrollments()) {
			GradebookDTO.Grade grade = new GradebookDTO.Grade();
			grade.name = e.getStudentName();
			grade.email = e.getStudentEmail();
			// does student have a grade for this assignment
			AssignmentGrade ag = assignmentGradeRepository.findByAssignmentIdAndStudentEmail(assignmentId,  grade.email);
			if (ag != null) {
				grade.grade = ag.getScore();
				grade.assignmentGradeId = ag.getId();
			} else {
				grade.grade = "";
				AssignmentGrade agNew = new AssignmentGrade(assignment, e);
				agNew = assignmentGradeRepository.save(agNew);
				grade.assignmentGradeId = agNew.getId();  // key value generated by database on save.
			}
			gradebook.grades.add(grade);
		}
		return gradebook;
	}
	
	@PostMapping("/course/{course_id}/finalgrades")
	@Transactional
	public void calcFinalGrades(@PathVariable int course_id) {
		System.out.println("Gradebook - calcFinalGrades for course " + course_id);
		
		// check that this request is from the course instructor 
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		Course c = courseRepository.findByCourse_id(course_id);
		if (!c.getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		CourseDTOG cdto = new CourseDTOG();
		cdto.course_id = course_id;
		cdto.grades = new ArrayList<>();
		for (Enrollment e: c.getEnrollments()) {
			double total=0.0;
			int count = 0;
			for (AssignmentGrade ag : e.getAssignmentGrades()) {
				count++;
				total = total + Double.parseDouble(ag.getScore());
			}
			double average = total/count;
			CourseDTOG.GradeDTO gdto = new CourseDTOG.GradeDTO();
			gdto.grade=letterGrade(average);
			gdto.student_email=e.getStudentEmail();
			gdto.student_name=e.getStudentName();
			cdto.grades.add(gdto);
			System.out.println("Course="+course_id+" Student="+e.getStudentEmail()+" grade="+gdto.grade);
		}
		
		registrationService.sendFinalGrades(course_id, cdto);
	}
	
	private String letterGrade(double grade) {
		if (grade >= 90) return "A";
		if (grade >= 80) return "B";
		if (grade >= 70) return "C";
		if (grade >= 60) return "D";
		return "F";
	}
	
	@PutMapping("/gradebook/{id}")
	@Transactional
	public void updateGradebook (@RequestBody GradebookDTO gradebook, @PathVariable("id") Integer assignmentId ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		checkAssignment(assignmentId, email);  // check that user name matches instructor email of the course.
		
		// for each grade in gradebook, update the assignment grade in database 
		
		for (GradebookDTO.Grade g : gradebook.grades) {
			AssignmentGrade ag = assignmentGradeRepository.findById(g.assignmentGradeId);
			if (ag == null) {
				throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid grade primary key. "+g.assignmentGradeId);
			}
			ag.setScore(g.grade);
			assignmentGradeRepository.save(ag);
		}
		
	}
	
	// As an instructor, I can change the name of the assignment for my course.
	@PutMapping("/gradebook/edit-name/{id}")
	public void editAssignmentName (@RequestBody AssignmentDTO assignments, @PathVariable("id") Integer assignmentId ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		checkAssignment(assignmentId, email);  // check that user name matches instructor email of the course.
		
			Assignment a = assignmentRepository.findById(assignments.assignmentId);
			if (a == null) {
				throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid assignment");
			}
			a.setName(assignments.assignmentName.toString());
			assignmentRepository.save(a);
		
	}
	
//	@PostMapping("gradebook/change-name")
//	@Transactional
//	public AssignmentDTO editAssignmentName(@RequestBody AssignmentDTO assignmentDTO) {
//		Assignment assignment = assignmentRepository.findById(assignmentDTO.assignmentId);
//
//        if (assignment != null) {
//            assignment.setName(assignment);
//            studentRepository.save(student);
//
//            return createAssignmentDTO(student);
//        } else {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Assignment");
//        }
//	}
	
	private Assignment checkAssignment(int assignmentId, String email) {
		// get assignment 
		Assignment assignment = assignmentRepository.findById(assignmentId);
		if (assignment == null) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Assignment not found. "+assignmentId );
		}
		// check that user is the course instructor
		if (!assignment.getCourse().getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		return assignment;
	}

}
