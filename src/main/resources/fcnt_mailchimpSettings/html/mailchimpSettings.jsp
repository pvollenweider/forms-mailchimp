<div class="row">
    <div class="col-lg-offset-6 col-md-6 text-right">
        <switch ng-change=msc.updateMailchimpConfiguration() ng-model="msc.mailchimpEnabled"></switch>
    </div>

    <div class="col-md-12 form-group" ng-if="msc.mailchimpEnabled"
         ng-class="{'has-error': mailchimpApiKeyForm['apiKey'].$invalid}">
        <form name="mailchimpApiKeyForm" ng-submit="msc.onSubmit('apiKey')">
            <label class="control-label">
                <span message-key="ff.label.mailchimp.apiKey"></span>
            </label>
            <div class="input-group">
                <input type="text"
                       class="form-control"
                       name="apiKey"
                       ng-model="msc.apiKey"
                       ng-required="true"/>
                <span class="input-group-btn">
                    <button type="submit"
                            class="btn btn-primary"
                            ng-disabled="mailchimpApiKeyForm['apiKey'].$invalid">
                        <span message-key="ff.label.ok"></span>
                    </button>
                </span>
            </div>
            <span class="help-block" ng-show="mailchimpApiKeyForm['apiKey'].$invalid">
                <span message-key="ff.label.required"></span>
            </span>
        </form>
    </div>
    <div class="col-md-12 form-group" ng-if="msc.lists">
        <form name="mailchimpListForm" ng-submit="msc.onSubmit('listId')">
            <label class="control-label">
                <span message-key="ff.label.mailchimp.selectList"></span>
                <span class="cursorPointer"
                      style="padding-left:5px"
                      ng-click="msc.refreshLists()"
                      uib-tooltip="{{msc.i18nMessageGetter('ff.mailchimp.tooltip.refreshList')}}"
                      tooltip-placement="top">
                    <i class="fa fa-refresh fa-lg"></i>
                </span>
            </label>
            <div class="input-group">
                <select class="form-control"
                        name="selectList"
                        ng-model="msc.listId"
                        ng-required="true"
                        ng-disabled="!msc.hasLists()"
                        ng-options="listName as id for (listName, id) in msc.lists">
                    <option ng-if="msc.listId == '' || msc.listId == null" value="" message-key="ff.label.mailchimp.placeholder.selectList"></option>
                </select>
                <span class="input-group-btn">
                    <button type="submit"
                            class="btn btn-primary"
                            ng-disabled="mailchimpListForm['selectList'].$invalid">
                        <span message-key="ff.label.ok"></span>
                    </button>
                    <span class="help-block" ng-show="mailchimpListForm['selectList'].$invalid">
                        <span message-key="ff.label.required"></span>
                    </span>
                </span>
            </div>
        </form>
    </div>
    <div class="col-md-12" ng-if="msc.apiKeyValid && !msc.hasLists()">
        <span ng-bind-html="msc.getEmptyListMessage()"></span>
    </div>
</div>
<hr/>