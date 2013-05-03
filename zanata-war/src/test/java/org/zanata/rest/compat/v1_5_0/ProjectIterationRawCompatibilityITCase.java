/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.rest.compat.v1_5_0;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.dbunit.operation.DatabaseOperation;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.zanata.RestTest;
import org.zanata.provider.DBUnitProvider;
import org.zanata.rest.ResourceRequest;
import org.zanata.v1_5_0.rest.MediaTypes;
import org.zanata.v1_5_0.rest.client.IProjectIterationResource;
import org.zanata.v1_5_0.rest.dto.ProjectIteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zanata.provider.DBUnitProvider.DataSetOperation;
import static org.zanata.util.RawRestTestUtils.assertJsonUnmarshal;
import static org.zanata.util.RawRestTestUtils.jsonMarshal;
import static org.zanata.util.RawRestTestUtils.jsonUnmarshal;

public class ProjectIterationRawCompatibilityITCase extends RestTest
{

   @Override
   protected void prepareDBUnitOperations()
   {
      addBeforeTestOperation(new DataSetOperation("org/zanata/test/model/ProjectsData.dbunit.xml", DatabaseOperation.CLEAN_INSERT));
   }
   
   @Test
   @RunAsClient
   public void getJsonProjectIteration() throws Exception
   {
      new ResourceRequest(getRestEndpointUrl("/projects/p/sample-project/iterations/i/1.0"), "GET")
      {
         @Override
         protected void prepareRequest(ClientRequest request)
         {
            request.header(HttpHeaders.ACCEPT, MediaTypes.APPLICATION_ZANATA_PROJECT_ITERATION_JSON);
         };
         
         @Override
         protected void onResponse(ClientResponse response)
         {
            assertThat(response.getStatus(), is(Status.OK.getStatusCode())); // 200
            assertJsonUnmarshal(response, ProjectIteration.class);
            
            ProjectIteration it = jsonUnmarshal(response, ProjectIteration.class);
            assertThat(it.getId(), is("1.0"));
         }
         
      }.run();
   }

   @Test
   @RunAsClient
   public void putJsonProjectIteration() throws Exception
   {
      final ProjectIteration newIteration = new ProjectIteration("new-iteration");
      
      new ResourceRequest(getRestEndpointUrl("/projects/p/sample-project/iterations/i/" + newIteration.getId()),
            "PUT", getAuthorizedEnvironment())
      {
         @Override
         protected void prepareRequest(ClientRequest request) 
         {
            request.body( MediaTypes.APPLICATION_ZANATA_PROJECT_ITERATION_JSON, jsonMarshal(newIteration) );
         };
         
         @Override
         protected void onResponse(ClientResponse response)
         {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode())); // 201
         }
         
      }.run();
      
      
      // Retreive it again
      IProjectIterationResource iterationClient = super.createProxy(createClientProxyFactory(TRANSLATOR, TRANSLATOR_KEY),
            IProjectIterationResource.class, "/projects/p/sample-project/iterations/i/" + newIteration.getId());

      ClientResponse<ProjectIteration> getResponse = iterationClient.get();
      
      assertThat(getResponse.getStatus(), is(Status.OK.getStatusCode())); // 200
      
      ProjectIteration it = getResponse.getEntity();
      assertThat(it.getId(), is("new-iteration"));
   }
}
