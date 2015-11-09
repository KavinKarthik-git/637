/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.web.api.dto;

import com.wordnik.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlType;

/**
 * Details for the login configuration.
 */
@XmlType(name = "loginConfig")
public class LoginConfigurationDTO {

    private Boolean supportsLogin;

    /**
     * @return Indicates whether or not this NiFi supports user login.
     */
    @ApiModelProperty(
            value = "Indicates whether or not this NiFi supports user login.",
            readOnly = true
    )
    public Boolean getSupportsLogin() {
        return supportsLogin;
    }

    public void setSupportsLogin(Boolean supportsLogin) {
        this.supportsLogin = supportsLogin;
    }
}
