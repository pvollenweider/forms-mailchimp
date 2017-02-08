<div class="row">
    <div class="col-md-12">
        <div class="form-group" ng-if="!mcc.invalidConfiguration">
            <label class="control-label">
                <span message-key="ff.label.mailchimp.mapInput"></span>
            </label>
            <div class="input-group">
            <select class="form-control"
                    name="selectList"
                    ng-model="mcc.selectedMergeField"
                    ng-disabled="!mcc.hasMergeFields()"
                    ng-options="mergeField as mergeField.name for mergeField in mcc.mergeFields track by mergeField.tag">
            </select>
            <span class="input-group-btn">
                <button ng-click="mcc.mapInput()"
                        ng-disabled="!mcc.canMapInput()"
                        class="btn btn-primary">
                    <span message-key="ff.label.mailchimp.map"></span>
                </button>
            </span>
            </div>
        </div>
        <div class="alert alert-danger" ng-if="mcc.invalidConfiguration">
            <span message-key="ff.label.mailChimp.message.alert.invalidConfiguration"/>
        </div>
    </div>
</div>