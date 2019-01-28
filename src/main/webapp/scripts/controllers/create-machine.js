'use strict';

(function(){

	var app = angular.module('topcat');

	app.controller('CreateMachineController', function($q, $scope, $state, $uibModal, $uibModalInstance, $timeout, $translate, tc, inform){
		var that = this;
        var facility = tc.facility($state.params.facilityName);
        var user = facility.user();
        var daaas = user.daaas();
		var timeout = $q.defer();
        var delaySeconds = user.daaas().config().createMachineDelaySeconds;
        $scope.$on('$destroy', function(){ timeout.resolve(); });

        this.machineTypeId = null;

        this.machineTypes = [];
        daaas.machineTypes(timeout).then(function(machineTypes){
            that.machineTypes = machineTypes;
        });

    	$uibModalInstance.rendered.then(function(){
    		that.open = true;
    	});

        this.create = function() {
            $uibModalInstance.close();
            var loading = $uibModal.open({
                templateUrl: daaas.pluginUrl() + 'views/loading.html',
                size : 'sm'
            });
            $timeout(function(){
                loading.dismiss('cancel');
            }, delaySeconds * 1000);
            daaas.createMachine(this.machineTypeId).then(function(){}, function(response){
                $uibModalInstance.dismiss('cancel');
                if (response != null && response['message'] != null) {
                    var message = response['message']
                } else {
                    var message = "Error creating analysis environment. Please try again later."
                }
                inform.add(message, {
                    'ttl': 0,
                    'type': 'info'
                });
            });
        };

    	this.close = function() {
            $uibModalInstance.dismiss('cancel');
        };
	});

})();
