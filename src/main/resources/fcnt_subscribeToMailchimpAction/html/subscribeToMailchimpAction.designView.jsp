<input type="hidden"
       class="form-control"
       ng-model="action.mappedEmailInput"
       field-name="mappedEmailInput"
       validation-type="any"
       ff-action-validator>
<div class="side-panel-body-content">
    <div class="row" style="padding: 20px;">
            <div class="form-group">
                <div class="col-sm-12">
                    <label class="control-label">
                        <span message-key="ff.label.action.subscribeToMailchimp.memberEmailMapping"></span>
                    </label>
                    <div class="panel-group" id="ffMailchimpSubscriberEmailMapperAccordion" role="tablist" aria-multiselectable="true">
                        <div class="panel panel-default" ng-repeat="step in stmc.steps">
                            <div class="panel-heading" role="tab" id="heading_{{$index}}">
                                <h4 class="panel-title" style="display:inline">
                                    <a role="button"
                                       data-toggle="collapse"
                                       data-parent="#ffMailchimpSubscriberEmailMapperAccordion"
                                       href="#collapse_{{$index}}"
                                       aria-expanded="true"
                                       aria-controls="collapse_{{$index}}">
                                        {{step.label}}
                                    </a>
                                </h4>
                            </div>
                            <div id="collapse_{{$index}}" class="panel-collapse collapse" role="tabpanel"
                                 aria-labelledby="heading_{{$index}}">
                                <div class="panel-body">
                                    <div class="inputName cursorPointer text-left"
                                         ng-class="{'mappedInput': action.mappedEmailInput == input.name}"
                                         ng-repeat="input in step.inputs"
                                         ng-click="stmc.updateMappedEmailInput(input.name)">
                                        {{input.label}}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>