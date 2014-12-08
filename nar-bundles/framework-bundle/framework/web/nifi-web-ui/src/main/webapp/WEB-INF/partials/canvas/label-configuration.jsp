<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<div id="label-configuration" class="dialog">
    <div>
        <div class="setting" style="margin-top: 5px;">
            <div class="setting-name">Label Value</div>
            <div class="setting-field">
                <textarea cols="30" rows="4" id="label-value" class="setting-input"></textarea>
            </div>
        </div>
        <div class="setting" style="margin-top: 5px; margin-bottom: 40px;">
            <div class="setting-name">Font Size</div>
            <div class="setting-field">
                <div id="label-font-size"></div>
            </div>
        </div>
    </div>
    <div id="label-configuration-button-container">
        <div id="label-configuration-apply" class="button button-normal">Apply</div>
        <div id="label-configuration-cancel" class="button button-normal">Cancel</div>
        <div class="clear"></div>
    </div>
</div>