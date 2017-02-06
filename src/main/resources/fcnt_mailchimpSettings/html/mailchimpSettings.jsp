<div class="row">
    <div class="col-lg-offset-6 col-md-6 text-right">
        <switch ng-change=msc.updateMailchimpConfiguration() ng-model="msc.mailchimpEnabled"></switch>
    </div>

    <div class="col-md-12 form-group" ng-if="msc.mailchimpEnabled"
         ng-class="{'has-error': mailchimpConfigurationForm['apiKey'].$invalid}">
        <form name="mailchimpConfigurationForm" ng-submit="msc.onSubmit()">
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
                            ng-disabled="mailchimpConfigurationForm['apiKey'].$invalid">
                        <span message-key="ff.label.ok"></span>
                    </button>
                </span>
            </div>
            <span class="help-block" ng-show="mailchimpConfigurationForm['apiKey'].$invalid">
                <span message-key="ff.label.required"></span>
            </span>
        </form>
    </div>
</div>
<hr/>