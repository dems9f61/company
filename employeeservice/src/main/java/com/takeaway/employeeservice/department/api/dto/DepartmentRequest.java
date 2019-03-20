package com.takeaway.employeeservice.department.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.takeaway.employeeservice.department.service.DepartmentParameter;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

/**
 * User: StMinko
 * Date: 18.03.2019
 * Time: 17:43
 * <p/>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
@EqualsAndHashCode
public class DepartmentRequest
{
    // =========================== Class Variables ===========================
    // =============================  Variables  =============================

    @ApiModelProperty(example = "Human Resources (HR)")
    @NotBlank(message = "The department name must not be blank")
    private final String departmentName;

    // ============================  Constructors  ===========================

    @JsonCreator
    DepartmentRequest(@JsonProperty(value = "departmentName", required = true) String departmentName)
    {
        this.departmentName = departmentName;
    }

    public DepartmentParameter toDepartmentParameter()
    {
        return new DepartmentParameter(departmentName);
    }

    // ===========================  public  Methods  =========================
    // =================  protected/package local  Methods ===================
    // ===========================  private  Methods  ========================
    // ============================  Inner Classes  ==========================
    // ============================  End of class  ===========================
}