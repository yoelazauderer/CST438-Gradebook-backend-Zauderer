package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;

@SpringBootTest
public class EndToEndTestCreateAssignment {

	public static final String CHROME_DRIVER_FILE_LOCATION = "/Users/yoelazauderer/downloads/chromedriver";

	public static final String URL = "https://cst438grade-fe.herokuapp.com/";

	public static final int TEST_COURSE_ID = 40443;
	
	public static final String TEST_ASSIGNMENT_NAME = "Test Assignment";
	
	public static final String TEST_DUE_DATE = "11-11-1111";

	public static final int SLEEP_DURATION = 1000; // 1 second.

	@Autowired
	AssignmentRepository assignmentRepository;
	
	@Autowired
	CourseRepository courseRepository;

	@Test
	public void addAssignmentTest() throws Exception {

		// check if assignment exists
		Assignment x = null;
		do {
			x = assignmentRepository.findByCourseIdAndName(TEST_COURSE_ID, TEST_ASSIGNMENT_NAME);
			if (x != null)
				assignmentRepository.delete(x);
		} while (x != null);

		// set the driver location and start driver
		//@formatter:off
		// browser	property name 				Java Driver Class
		// edge 	webdriver.edge.driver 		EdgeDriver
		// FireFox 	webdriver.firefox.driver 	FirefoxDriver
		// IE 		webdriver.ie.driver 		InternetExplorerDriver
		//@formatter:on

		System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
		WebDriver driver = new ChromeDriver();
		// Puts an Implicit wait for 10 seconds before throwing exception
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		try {

			driver.get(URL);
			Thread.sleep(SLEEP_DURATION);
			
			// locate and click "Add Assignment Button"
			driver.findElement(By.xpath("//button")).click();
			Thread.sleep(SLEEP_DURATION);
			
			// enter course_id
			driver.findElement(By.xpath("//input[@name='courseId']")).sendKeys(Integer.toString(TEST_COURSE_ID));
			
			//enter assignment name
			driver.findElement(By.xpath("//input[@name='assignmentName']")).sendKeys(TEST_ASSIGNMENT_NAME);
			
			//enter due date
			driver.findElement(By.xpath("//input[@name='dueDate']")).sendKeys(TEST_DUE_DATE);
			
			//click "Add"
			driver.findElement(By.xpath("//button[span='Add']")).click();
			Thread.sleep(SLEEP_DURATION);
			
			//verify that new assignment shows in assignment list			
			WebElement we = driver.findElement(By.xpath("(//div[@role='row'])[last()]"));
			assertNotNull(we, "Added assignment does not show in assignment list.");
			
			//verify that assignment row has been inserted to database
			Assignment a = assignmentRepository.findByCourseIdAndName(TEST_COURSE_ID, TEST_ASSIGNMENT_NAME);
			assertNotNull(a, "Assignment not found in database.");

		} catch (Exception ex) {
			throw ex;
		} finally {

			// clean up database.
			Assignment a = assignmentRepository.findByCourseIdAndName(TEST_COURSE_ID, TEST_ASSIGNMENT_NAME);
			if (a != null)
				assignmentRepository.delete(a);

			driver.quit();
		}

	}
	
}
