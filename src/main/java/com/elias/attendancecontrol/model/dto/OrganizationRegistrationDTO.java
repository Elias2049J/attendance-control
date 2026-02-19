package com.elias.attendancecontrol.model.dto;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.User;
import lombok.Data;
@Data
public class OrganizationRegistrationDTO {
    private Organization organization;
    private User user;
    public OrganizationRegistrationDTO() {
        this.organization = new Organization();
        this.user = new User();
    }
}
