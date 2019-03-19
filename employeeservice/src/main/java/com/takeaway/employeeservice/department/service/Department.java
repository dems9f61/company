package com.takeaway.employeeservice.department.service;

import com.takeaway.employeeservice.employee.service.Employee;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * User: StMinko
 * Date: 18.03.2019
 * Time: 11:30
 * <p/>
 */

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Table(name = "DEPARTMENTS")
public class Department
{
    // =========================== Class Variables ===========================
    // =============================  Variables  =============================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "department_name", length = 50, nullable = false, unique = true)
    private String departmentName;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<Employee> employees = new HashSet<>();

    // ============================  Constructors  ===========================
    // ===========================  public  Methods  =========================
    // =================  protected/package local  Methods ===================
    // ===========================  private  Methods  ========================
    // ============================  Inner Classes  ==========================
    // ============================  End of class  ===========================
}
