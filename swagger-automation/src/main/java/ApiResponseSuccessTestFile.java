import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwn.plife.common.Constants;
import com.gwn.plife.common.dme.device.DisplayDevice;
import com.gwn.plife.common.dme.user.Patient;
import com.gwn.plife.common.dme.user.User;
import com.gwn.plife.common.dto.DeviceCommandLogDTO;
import com.gwn.plife.common.dto.DeviceCommandLogDataDTO;
import com.gwn.plife.common.dto.DeviceCommandLogResponseDTO;
import com.gwn.plife.log.dme.DeviceCommandLog;
import com.gwn.plife.log.dme.DeviceCommandLogData;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

import javax.annotation.security.PermitAll;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * This file tests the functionality of successfully adding a swagger @ApiResponse annotation. The expected behavior
 * for this file is for an @ApiResponse annotation to be added and no TODO comment stating that it should be added
 * Additionally, an import statement for @ApiResponse annotations should be added to the top of this file. The
 *
 * @StatusCodes and @ResponseCode annotations should also be removed along with their import statements since the
 * equivalent swagger annotations are being added to the file.
 * (among other things)
 */

@Path("/batchJob")
// TODO Fix permissions
@PermitAll

public class ApiResponseSuccessTestFile {


    EntityManager em;
    Logger logger;

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
     * @param parameter The parameter to be used
     */
    @PUT
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition = "required parameter is not provided in the request," + "or the location with a free device cannot be found"), @ResponseCode(code = 404, condition = "Not found")})
    public void toTest(String parameter) {
        String test = parameter;
    }


    /**
     * Prepare PlcActivity data and return a bulky DTO
     *
     * @param startDate            date range start value
     * @param endDate              date range end value
     * @param deviceCommandLogList list of {@link DeviceCommandLog}
     * @return {@link DeviceCommandLogResponseDTO}
     */
    private DeviceCommandLogResponseDTO prepareDeviceCommandLogData(Date startDate, Date endDate, List<DeviceCommandLog> deviceCommandLogList) {

        DeviceCommandLogResponseDTO deviceCommandLogResponseDTO = new DeviceCommandLogResponseDTO();
        List<DeviceCommandLogDTO> deviceCommandLogDTOList = new ArrayList<>();

        deviceCommandLogResponseDTO.setDateRangeEnd(Constants.longDateDisplayFormat.format(endDate));
        deviceCommandLogResponseDTO.setDateRangeStart(Constants.longDateDisplayFormat.format(startDate));

        if (deviceCommandLogList != null) {

            for (DeviceCommandLog deviceCommandLog : deviceCommandLogList) {
                List<DeviceCommandLogDataDTO> deviceCommandLogDataDTOList = new ArrayList<>();
                for (DeviceCommandLogData deviceCommandLogData : deviceCommandLog.getDeviceCommandLogData()) {

                    // This Device could be null, if someone manually deleted the DisplayDevice from database
                    DisplayDevice device = em.find(DisplayDevice.class, deviceCommandLogData.getDisplayDeviceId());
                    Patient patient = null;

                }

                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> commandParams = new HashMap<>();

                try {
                    // convert JSON string to Map
                    commandParams = mapper.readValue(deviceCommandLog.getCommandParams(), new TypeReference<Map<String, String>>() {

                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                User activityBy = em.find(User.class, deviceCommandLog.getUserId());
                deviceCommandLogDTOList.add(new DeviceCommandLogDTO(deviceCommandLog.getDeviceCommand(), activityBy.getUserName(), commandParams, Constants.longDateDisplayFormat.format(deviceCommandLog.getTimeStamp()), deviceCommandLogDataDTOList));
            }
            deviceCommandLogResponseDTO.setDeviceCommandLogDTOList(deviceCommandLogDTOList);
        }
        return deviceCommandLogResponseDTO;
    }


}


