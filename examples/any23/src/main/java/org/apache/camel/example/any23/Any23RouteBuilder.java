/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.any23;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.Any23Type;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Any23RouteBuilder extends RouteBuilder {

    private static final String BASE_URI = "http://mock.foo/bar";

    @Override
    public void configure() {

        from("direct:start")
                .log("Querying dbpedia:Ecuador ")
                .to("http://dbpedia.org/page/Ecuador")
                .unmarshal().any23(BASE_URI)
                .process(exchange -> {
                    ValueFactory vf = SimpleValueFactory.getInstance();
                    Model model = exchange.getIn().getBody(Model.class);

                    // Selecting the leaders of Ecuador
                    IRI propertyLeader = vf.createIRI("http://dbpedia.org/property/leaderName");
                    Set<Value> leadersResources = model.filter(null, propertyLeader, null).objects();
                    List<String> leadersList = new ArrayList<>();
                    for (Value leader : leadersResources) {
                        // Transform the leader resource (URI) into a browsable URL.
                        // For instance:
                        // http://dbpedia.org/resource/Oswaldo_Guayasam%C3%ADn -->
                        // http://dbpedia.org/page/Oswaldo_Guayasam%C3%ADn
                        String aLeader = leader.stringValue().replace("resource", "page");
                        leadersList.add(aLeader);
                    }
                    exchange.getIn().setBody(leadersList);

                })
                .log(" Content: ${body} ")
                // Process each leader in a separate route.
                // In order to extract more information.
                .split(simple("${body}"))
                .to("direct:extractMoreData");

        from("direct:extractMoreData")
                .id("result")
                .log("Split ${body}")
                .toD("${body}").unmarshal()
                // Extract RDF data of the leaders as JSONLD
                .any23(BASE_URI, Any23Type.JSONLD)
                .log(" Result : ${body} ")
                .to("log:result");
    }
}
