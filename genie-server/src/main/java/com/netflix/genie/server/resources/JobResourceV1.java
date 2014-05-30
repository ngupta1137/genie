/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.server.resources;

import com.netflix.genie.common.exceptions.CloudServiceException;
import com.netflix.genie.common.messages.JobRequest;
import com.netflix.genie.common.messages.JobResponse;
import com.netflix.genie.common.messages.JobStatusResponse;
import com.netflix.genie.common.model.ClusterCriteria;
import com.netflix.genie.common.model.Job;
import com.netflix.genie.server.services.ExecutionService;
import com.netflix.genie.server.services.ExecutionServiceFactory;
import com.netflix.genie.server.util.JAXBContextResolver;
import com.netflix.genie.server.util.ResponseUtil;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource class for executing and monitoring jobs via Genie.
 *
 * @author amsharma
 */
@Path("/v1/jobs")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class JobResourceV1 {

    private final ExecutionService xs;
    private static final Logger LOG = LoggerFactory.getLogger(JobResourceV1.class);

    /**
     * Custom JAXB context resolver based for the job request/responses.
     *
     * @author amsharma
     */
    @Provider
    public static class JobJAXBContextResolver extends JAXBContextResolver {

        /**
         * Constructor - initialize the resolver for the types that this
         * resource cares about.
         *
         * @throws Exception if there is any error in initialization
         */
        public JobJAXBContextResolver() throws Exception {
            super(new Class[]{JobRequest.class,
                JobStatusResponse.class,
                Job.class,
                JobResponse.class,
                ClusterCriteria.class});
        }
    }

    /**
     * Default constructor.
     *
     * @throws CloudServiceException
     */
    public JobResourceV1() throws CloudServiceException {
        xs = ExecutionServiceFactory.getExecutionServiceImpl();
    }

    /**
     * Submit a new job.
     *
     * @param request request object containing job info element for new job
     * @param hsr servlet context
     * @return successful response, or one with HTTP error code
     */
    @POST
    @Path("/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response submitJob(JobRequest request,
            @Context HttpServletRequest hsr) {
        // get client's host from the context
        String clientHost = hsr.getHeader("X-Forwarded-For");
        if (clientHost != null) {
            clientHost = clientHost.split(",")[0];
        } else {
            clientHost = hsr.getRemoteAddr();
        }
        LOG.info("called from: " + clientHost);

        // set the clientHost, if it is not overridden already
        Job job = request.getJobInfo();
        if ((job != null)
                && ((job.getClientHost() == null)
                || job.getClientHost().isEmpty())) {
            job.setClientHost(clientHost);
            job.setClusterCriteriaString(job.getClusterCriteriaList());
        }

        JobResponse response = xs.submitJob(request);
        return ResponseUtil.createResponse(response);
    }

    /**
     * Get job information for given job id.
     *
     * @param jobID id for job to look up
     * @return successful response, or one with HTTP error code
     */
    @GET
    @Path("/{jobID}")
    public Response getJobInfo(@PathParam("jobID") String jobID) {
        LOG.info("called for jobID: " + jobID);
        JobResponse response = xs.getJobInfo(jobID);
        return ResponseUtil.createResponse(response);
    }

    /**
     * Get job status for give job id.
     *
     * @param jobID id for job to look up
     * @return successful response, or one with HTTP error code
     */
    @GET
    @Path("/{jobID}/status")
    public Response getJobStatus(@PathParam("jobID") String jobID) {
        LOG.info("called for jobID" + jobID);
        JobStatusResponse response = xs.getJobStatus(jobID);
        return ResponseUtil.createResponse(response);
    }

    /**
     * Get job info for given filter criteria.
     *
     * @param jobID id for job
     * @param jobName name of job (can be a SQL-style pattern such as HIVE%)
     * @param userName user who submitted job
     * @param status status of job - possible types Type.JobStatus
     * @param clusterName the name of the cluster
     * @param clusterId the id of the cluster
     * @param limit max number of jobs to return
     * @param page page number for job
     * @return successful response, or one with HTTP error code
     */
    @GET
    @Path("/")
    public Response getJobs(@QueryParam("jobID") String jobID,
            @QueryParam("jobName") String jobName,
            @QueryParam("userName") String userName,
            @QueryParam("status") String status,
            @QueryParam("clusterName") String clusterName,
            @QueryParam("clusterId") String clusterId,
            @QueryParam("limit") @DefaultValue("1024") int limit,
            @QueryParam("page") @DefaultValue("0") int page) {

        LOG.info("called");

        JobResponse response = xs.getJobs(jobID, jobName, userName, status, clusterName, clusterId, limit, page);

        return ResponseUtil.createResponse(response);
    }

    /**
     * Kill job based on given job ID.
     *
     * @param jobID id for job to kill
     * @return successful response, or one with HTTP error code
     */
    @DELETE
    @Path("/{jobID}")
    public Response killJob(@PathParam("jobID") String jobID) {
        LOG.info("called for jobID: " + jobID);
        JobStatusResponse response = xs.killJob(jobID);
        return ResponseUtil.createResponse(response);
    }
}