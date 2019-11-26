import com.gwn.plife.common.StaticPermissions;
import com.gwn.plife.common.auth.ejb.AuthenticatorService;
import com.gwn.plife.common.dme.Permission;
import com.gwn.plife.common.dme.user.Admin;
import com.gwn.plife.common.dme.user.User;
import com.gwn.plife.user.ejb.PermissionsManager;
import com.gwn.plife.user.ejb.UserManager;
import com.gwn.plife.whiteboard.dto.EventDateDTO;
import com.gwn.pls.model.admin.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


/**
 * This file tests the functionality of successfully adding a swagger @Parameter annotation. The expected behavior
 * for this file is for a @Parameter annotation to be present and no TODO comment stating that it should be added.
 * Additionally, an import statement for @Parameter annotations should be added to the top of this file.
 * (among other things)
 */


@Path("/oauth")
@PermitAll
public class ParameterSuccessTestFile {


    private AuthenticatorService authenticatorService;


    private PermissionsManager permissionsManager;


    private UserManager userManager;


    private List<EventDateDTO> eventDates = new ArrayList<>();

    /**
     * Authenticate pls user credentials against configured auth plugins
     *
     * @param request  HttpServletRequest
     * @param userName pls username
     * @param password account password
     * @return User details with assigned roles (permissions)
     */
    @POST
    @Path("/authenticate/user")
    @Produces("application/json")
    @Consumes("application/x-www-form-urlencoded")
    @Operation(summary = "Authenticate pls user credentials against configured auth plugins")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public Response authenticate(@Context HttpServletRequest request, String userName, @FormParam("password") String password) {

        UserDTO userDTO = new UserDTO();
        Response.ResponseBuilder responseBuilder;
        Admin admin = authenticatorService.runSimpleAdminAuthPlugins(userName, password, null);
        if (admin != null) {
            userDTO.setRoles(getUserPermissions(admin));
            userDTO.setUserId(admin.getUserId());
            userDTO.setFirstName(admin.getFirstName());
            userDTO.setLastName(admin.getLastName());
            userDTO.setUserName(admin.getUserName());
            userDTO.setDomainId(admin.getDomainId());
            userDTO.setSuperUser(admin.isSuperUser());
            responseBuilder = Response.ok().entity(userDTO);
        } else {
            responseBuilder = Response.status(Response.Status.UNAUTHORIZED).entity(userDTO);
        }
        return responseBuilder.build();
    }


    /**
     * Get assigned roles (permissions) for a user
     *
     * @param request HttpServletRequest
     * @param userId  pls userId
     * @return Assigned roles (permissions) based on userId
     */
    @GET
    @Path("/roles/user/{userId}")
    @Produces("application/json")
    @RolesAllowed(StaticPermissions.viewAdminAccounts)
    @Operation(summary = "Get assigned roles (permissions) for a user")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Invalid UserId")
    public Response getUserRoles(@Context HttpServletRequest request, @PathParam("userId") String userId) {

        UserDTO userDTO = new UserDTO();
        Response.ResponseBuilder responseBuilder;
        User user = userManager.findUserById(userId);
        if (user != null && user.asAdmin() != null) {
            Admin admin = user.asAdmin();
            userDTO.setRoles(getUserPermissions(admin));
            userDTO.setUserId(admin.getUserId());
            userDTO.setFirstName(admin.getFirstName());
            userDTO.setLastName(admin.getLastName());
            userDTO.setUserName(admin.getUserName());
            userDTO.setDomainId(admin.getDomainId());
            userDTO.setSuperUser(admin.isSuperUser());
            responseBuilder = Response.ok().entity(userDTO);
        } else {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        }
        return responseBuilder.build();
    }

    /**
     * Getting the User Permissions
     *
     * @param admin
     * @return
     */
    private List<String> getUserPermissions(Admin admin) {

        List<String> userRoles = new ArrayList<>();
        if (admin.isSuperUser()) {
            List<Permission> permissions = permissionsManager.findSuperUserPermissions();
            for (Permission permission : permissions) {
                userRoles.add(permission.getPermission());
            }
        } else {
            Map<String, Calendar> result = permissionsManager.getCurrentPermissionSet(admin.getUserId());
            if (result != null && result.size() > 0) {
                userRoles.addAll(result.keySet());
            }
        }
        return userRoles;
    }


    public List<EventDateDTO> getEventDates() {

        return eventDates;
    }


}
