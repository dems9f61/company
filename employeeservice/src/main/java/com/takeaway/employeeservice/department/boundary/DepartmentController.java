package com.takeaway.employeeservice.department.boundary;

import com.takeaway.employeeservice.ApiVersions;
import com.takeaway.employeeservice.department.boundary.dto.DepartmentRequest;
import com.takeaway.employeeservice.department.boundary.dto.DepartmentResponse;
import com.takeaway.employeeservice.department.control.DepartmentParameter;
import com.takeaway.employeeservice.department.control.DepartmentServiceCapable;
import com.takeaway.employeeservice.department.control.DepartmentServiceException;
import com.takeaway.employeeservice.department.entity.Department;
import com.takeaway.employeeservice.error.entity.BadRequestException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.HttpURLConnection;

/**
 * User: StMinko
 * Date: 18.03.2019
 * Time: 17:23
 * <p/>
 */
@Slf4j
@RestController
@Api(value = "Department service: Operations pertaining to department service interface")
@RequestMapping(DepartmentController.BASE_URI)
@RequiredArgsConstructor
public class DepartmentController
{
    // =========================== Class Variables ===========================

    public static final String BASE_URI = ApiVersions.V1+ "/departments";

    // =============================  Variables  =============================

    private final DepartmentServiceCapable departmentService;

    // ============================  Constructors  ===========================
    // ===========================  public  Methods  =========================

    @ApiOperation(value = "Creates a Department with the request values")
    @ApiResponses({ @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "") })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DepartmentResponse createDepartment(@RequestBody @NotNull @Valid DepartmentRequest departmentRequest)
    {
        DepartmentParameter createDepartmentParameter = departmentRequest.toDepartmentParameter();
        try
        {
            Department department = departmentService.create(createDepartmentParameter);
            return new DepartmentResponse(department.getId(), department.getDepartmentName());
        }
        catch (DepartmentServiceException caught)
        {
            throw new BadRequestException(caught.getMessage(), caught.getCause());
        }
    }

    // =================  protected/package local  Methods ===================
    // ===========================  private  Methods  ========================
    // ============================  Inner Classes  ==========================
    // ============================  End of class  ===========================
}