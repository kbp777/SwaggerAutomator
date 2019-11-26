import com.gwn.plife.accessory.dto.AccessoryRegistrationRequestDTO;
import com.gwn.plife.accessory.dto.AccessoryResponseDTO;
import com.gwn.plife.accessory.ejb.AccessoryManager;
import com.gwn.plife.common.StaticPermissions;
import com.gwn.pls.rest.GWNRestException;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * This file tests the functionality of successfully adding a swagger @Operation annotation. The expected behavior
 * for this file is for a @Operation annotation to be present and no TODO comment stating that it should be added.
 * Additionally, an import statement for @Operation annotations should be added to the top of this file.
 * (among other things)
 */


@Path("/batchJob")
// TODO Fix permissions
@PermitAll

public class OperationSuccessTestFile {

    private AccessoryManager accessoryManager;

    @Context
    SecurityContext securityContext;


    /**
     * This method is to be used for testing only.
     *
     * @param str1 The string to be used
     * @param int1 The integer to be used
     */
    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition = "required parameter is not provided in the request," + "or the location with a free device cannot be found"), @ResponseCode(code = 404, condition = "Not found")})
    public void isRegister(String str1, Integer int1) {

    }


    /**
     * This method is used for testing.
     *
     * @param parameter The string to be used
     * @param int1      The integer to be used
     */
    @PUT
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void toTest(String parameter, Integer int1) {
        String test = parameter;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({StaticPermissions.accessoryRegistration})
    @Operation(summary = "Register an accessory to a room or bed", description = "Register the Accessory described in the input")
    @ApiResponse(description = "accessory information after registration with relevant configuration", content = @Content(schema = @Schema(implementation = AccessoryResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Required parameter is not provided in the request")
    public AccessoryResponseDTO registerAccessory(@Parameter(description = "A description of the Accessory to be Registered", schema = @Schema(implementation = AccessoryRegistrationRequestDTO.class, required = true)) AccessoryRegistrationRequestDTO accessoryRegistrationRequestDTO) throws GWNRestException {
        // api user
        String loggedInUser = securityContext.getUserPrincipal().getName();
        AccessoryResponseDTO accessoryResponseDTO = accessoryManager.registerAccessory(accessoryRegistrationRequestDTO, loggedInUser);
        if (accessoryResponseDTO.getErrors() != null && !accessoryResponseDTO.getErrors().isEmpty()) {
            throw new GWNRestException("Accessory registration failed.", accessoryResponseDTO.getErrors().toArray(new String[0]), Response.Status.BAD_REQUEST);
        }
        return accessoryResponseDTO;
    }

}


