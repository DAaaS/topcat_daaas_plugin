
(function() {
    'use strict';

    var app = angular.module('topcat');

    app.controller('AdminMachinesController', function($scope, $state, $uibModal, $q, $interval){
    	var that = this;
      this.facilities = tc.adminFacilities();

      if(!$state.params.facilityName){
        $state.go('admin.machines', {facilityName: this.facilities[0].config().name});
        return;
      }

      var facility = tc.facility($state.params.facilityName);
      var admin = facility.admin();
      var daaas = admin.daaas();
      var timeout = $q.defer();
      $scope.$on('$destroy', function(){ timeout.resolve(); });

      this.state = 'aquired';
      this.host = '';
      this.machines = [];

      this.update = function(){
        daaas.machines(timeout.promise, [
          "where 1 = 1",
          function(){
            if(that.state != 'any'){
              return ["and machine.state like concat(?, '%') ", that.state];
            }
          },
          function(){
            if(that.host != ''){
              return ["and machine.host like concat('%', ?, '%') ", that.host];
            }
          }
        ]).then(function(machines){
          that.machines = machines;

          _.each(machines, function(machine){
              _.each(machine.users,  function(user){

                  if(user.userName == facility.icat().session().username && user.type == 'SECONDARY'){
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
        });
      };

      this.update();

      this.enableAccess = function(machine){
        machine.enableAccess(timeout.promise).then(function(){
          that.update();
        });
      };

      this.disableAccess = function(machine){
        machine.disableAccess(timeout.promise).then(function(){
          that.update();
        });
      };

      this.view = function(machine){
        window.open(daaas.pluginUrl() + 'views/vnc.html?facilityName=' + encodeURIComponent($state.params.facilityName)  + '&id=' + machine.id, '_blank', 'height=600,width=800,scrollbars=no,status=no');
      };

    });

})();