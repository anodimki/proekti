package mk.ukim.finki.wp.kol2022.g2.service.impl;

import mk.ukim.finki.wp.kol2022.g2.model.Course;
import mk.ukim.finki.wp.kol2022.g2.model.Student;
import mk.ukim.finki.wp.kol2022.g2.model.StudentType;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidCourseIdException;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidStudentIdException;
import mk.ukim.finki.wp.kol2022.g2.repository.CourseRepository;
import mk.ukim.finki.wp.kol2022.g2.repository.StudentRepository;
import mk.ukim.finki.wp.kol2022.g2.service.StudentService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StudentServiceImpl implements StudentService, UserDetailsService {

    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;

    public StudentServiceImpl(CourseRepository courseRepository, PasswordEncoder passwordEncoder, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentRepository = studentRepository;
    }

    @Override
    public List<Student> listAll() {
        return this.studentRepository.findAll();
    }

    @Override
    public Student findById(Long id) {
        return this.studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
    }

    @Override
    public Student create(String name, String email, String password, StudentType type, List<Long> courseId, LocalDate enrollmentDate) {
        return this.studentRepository.save(new Student(name,email,passwordEncoder.encode(password),type,this.courseRepository.findAllById(courseId),enrollmentDate));
    }

    @Override
    public Student update(Long id, String name, String email, String password, StudentType type, List<Long> coursesId, LocalDate enrollmentDate) {
        Student student = this.findById(id);
        student.setName(name);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(password));
        student.setType(type);
        student.setCourses(this.courseRepository.findAllById(coursesId));
        student.setEnrollmentDate(enrollmentDate);
        return this.studentRepository.save(student);
    }

    @Override
    public Student delete(Long id) {
        Student student = this.findById(id);
        this.studentRepository.delete(student);
        return student;
    }

    @Override
    public List<Student> filter(Long courseId, Integer yearsOfStudying) {
        if(courseId == null && yearsOfStudying == null){
            return this.studentRepository.findAll();
        }else if(courseId == null){
            return this.studentRepository.findAll().stream().filter(student -> LocalDate.now().getYear() - student.getEnrollmentDate().getYear() > yearsOfStudying).collect(Collectors.toList());
        }else if(yearsOfStudying == null)
        {
            Course course = this.courseRepository.findById(courseId).orElseThrow(InvalidCourseIdException::new);
            return this.studentRepository.findByCourses(course);
        }else {
            Course course = this.courseRepository.findById(courseId).orElseThrow(InvalidCourseIdException::new);
            return this.studentRepository.findByCourses(course).stream().filter(student -> LocalDate.now().getYear() - student.getEnrollmentDate().getYear() > yearsOfStudying).collect(Collectors.toList());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Student student = this.studentRepository.findByEmail(username).orElseThrow(()-> new UsernameNotFoundException(username));
        return new org.springframework.security.core.userdetails.User(
                student.getEmail(),
                student.getPassword(),
                Stream.of(new SimpleGrantedAuthority("ROLE_"+student.getType().toString())).collect(Collectors.toList()));
    }
}
