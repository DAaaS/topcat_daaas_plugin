'use strict';

(function(){

	var app = angular.module('topcat');

	app.controller('MyMachinesController', function($q, $scope, $uibModal, $interval, $state, tc, inform){
		var that = this;
        this.facilities = tc.userFacilities();

        if(!$state.params.facilityName){
            $state.go('home.my-machines', {facilityName: this.facilities[0].config().name});
            return;
        }

        var facility = tc.facility($state.params.facilityName);
        var user = facility.user();
        var daaas = user.daaas();
		var timeout = $q.defer();
    	$scope.$on('$destroy', function(){ timeout.resolve(); });

	    this.machines = [];
        var machinesHash = JSON.stringify(this.machines);

        function pollMachines(){
            timeout = $q.defer();
            timeout.promise.then(function(){ $interval.cancel(pollMachinesPromise); });
            daaas.machines({bypassInterceptors: true, timeout: timeout.promise}).then(function(machines){
                var currentMachinesHash = JSON.stringify(machines);
                if(currentMachinesHash != machinesHash){
                    that.machines = machines;
                    machinesHash = currentMachinesHash;

                    _.each(machines, function(machine){
                        _.each(machine.users,  function(user){
                            if(user.userName == facility.icat().session().username){
                                machine.type = user.type;
                            }

                            if(user.type == 'PRIMARY'){
                                facility.icat().query(timeout.promise, [
                                    "select user from User user",
                                    "where user.name = ?", user.userName
                                ]).then(function(users){
                                    if(users[0]){
                                        machine.primaryUser = users[0];
                                    }
                                });
                            }
                        });
                    });
                }
            });
        }
        var pollMachinesPromise = $interval(pollMachines, 5000);
        pollMachines();
	    

	    this.view = function(machine){
            window.open(daaas.pluginUrl() + 'views/vnc.html?facilityName=' + encodeURIComponent($state.params.facilityName)  + '&id=' + machine.id, '_blank', 'height=600,width=800,scrollbars=no,status=no');
        };

        this.delete = function(machine){
            if(confirm("Are you really sure you want to delete this machine?")){
                daaas.deleteMachine(timeout.promise, machine.id).then(pollMachines);
            }
        };

        this.create = function(machine){
            that.machine = machine;
            $uibModal.open({
                templateUrl: daaas.pluginUrl() + 'views/create-machine.html',
                controller: 'CreateMachineController as createMachineController',
                size : 'lg',
                scope: $scope
            });
        };

        this.share = function(machine){
            that.machine = machine;
            $uibModal.open({
                templateUrl: daaas.pluginUrl() + 'views/share-machine.html',
                controller: 'ShareMachineController as shareMachineController',
                size : 'md',
                scope: $scope
            });
        };

	});

})();
