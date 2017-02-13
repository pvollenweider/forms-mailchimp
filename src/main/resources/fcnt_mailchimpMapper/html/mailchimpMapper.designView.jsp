<div class="row">
    <div class="col-md-12">
        <div class="form-group" ng-if="!mcc.invalidConfiguration">
            <label class="control-label">
                <span message-key="ff.label.mailchimp.mapInput"></span>
            </label>
            <select class="form-control"
                    name="selectList"
                    ng-model="mcc.selectedMergeField"
                    ng-click="oldValue = mcc.selectedMergeField"
                    ng-change="mcc.mapInput(oldValue)"
                    ng-disabled="!mcc.hasMergeFields()"
                    ng-options="mergeField as mergeField.name for mergeField in mcc.mergeFields track by mergeField.tag">
            </select>
        </div>
        <div class="alert alert-danger" ng-if="mcc.invalidConfiguration">
            <span message-key="ff.label.mailChimp.message.alert.invalidConfiguration"/>
        </div>
    </div>
</div>
<div class="row">
    <div class="col-sm-2 col-sm-offset-10">
        <button class="btn btn-link pull-right" ng-click="mcc.reset()">
            <span message-key="ff.label.mailChimp.message.label.reset"/>
        </button>
    </div>
</div>