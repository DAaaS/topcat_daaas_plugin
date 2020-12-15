import KeyTable from "/topcat_daaas_plugin/bower_components/noVNC/core/input/keysym.js";

// RFB holds the API to connect and communicate with a VNC server
import RFB from '/topcat_daaas_plugin/bower_components/noVNC/core/rfb.js';

let rfb;
let desktopName;

// When this function is called we have
// successfully connected to a server
function connectedToServer(e) {
    var str = desktopName.substring(0, 20) + desktopName.substring(22, desktopName.length);
    console.log(desktopName);
}

// This function is called when we are disconnected
function disconnectedFromServer(e) {
    $("#reconnect").show();
}

// When this function is called we have received
// a desktop name from the server
function updateDesktopName(e) {
    desktopName = e.detail.name;
}


function connect() {
    // Creating a new RFB object will start a new connection
    let params = new URLSearchParams(location.search);
    var id = params.get('id');
    var connection_url = localStorage.getItem(id);
    rfb = new RFB(document.getElementById('screen'), connection_url, { credentials: { password: "" }, wsProtocols: ['binary'] });
    if (window.allowresize) {
        rfb.resizeSession = true;
        rfb.background = 'rgb(200,200,200)';
    }
    rfb.qualityLevel = 9;

    $("#reconnect").hide();

    // Add listeners to important events from the RFB module
    rfb.addEventListener("connect", connectedToServer);
    rfb.addEventListener("disconnect", disconnectedFromServer);
    rfb.addEventListener("desktopname", updateDesktopName);
}

function reconnect() {
    // Directly calling connect() with reconnect click breaks the top box
    // So just refresh the page to attempt reconnect
    location.reload();

}

// Establish first connection
connect();

$("#reconnect_but").on('click', reconnect);

//When the page is loaded, begin loading in clipboard controls
$(document).ready(function () {
    var remoteHighlight = "";
    //Detect user is on mac
    var mac = false;
    if (navigator.appVersion.indexOf("Mac") != -1) mac = true;

    rfb.addEventListener("connect", loadCanvasClipboardControls);

    // The copy/paste using shortcut keys works as follows
    // Listen for keypress events on ctrl, c, & v
    // When it detects a combo that would initiate paste, make the noVNC read-only
    // Making noVNC read-only means we can catch the onPaste event in the window
    // Get the clipboard data, and send it to the machine
    // Make noVNC read-write again
    // Simulate whatever combo was initially detected, on the remote machine
    function loadCanvasClipboardControls() {
        rfb.addEventListener("clipboard", clipboardReceive);
        rfb.clipboardPasteFrom("");
        // lock and unlock noVNC to "initalise" them
        // Without these calls, the first paste always fails
        lock();
        // setTimeout moves operation to end of queue of tasks
        // this means that 1ms is sufficient
        setTimeout(unlock, 1);
        var ctrlDown = false,
            cmdDown = false,
            ctrlKey = 17,
            lcmdKey = 91,
            rcmdKey = 93,
            firefoxcmdkey = 224,
            vKey = 86,
            xKey = 88,
            cKey = 67;

        // brings focus to vnc canvas once it's loaded
        $("canvas").focus();

        // add eventlisteners to detect when ctrl/cmd are held down
        $("canvas").keydown(function (e) {
            if (e.keyCode == ctrlKey) ctrlDown = true;
            if (mac && (e.keyCode == lcmdKey || e.keyCode == rcmdKey || e.keyCode == firefoxcmdkey)) cmdDown = true;
        }).keyup(function (e) {
            if (e.keyCode == ctrlKey) ctrlDown = false;
            if (mac && (e.keyCode == lcmdKey || e.keyCode == rcmdKey || e.keyCode == firefoxcmdkey)) cmdDown = false;
        });

        $(".no-copy-paste").keydown(function (e) {
            if (ctrlDown && (e.keyCode == vKey || e.keyCode == cKey)) return false;
            if (cmdDown && (e.keyCode == vKey || e.keyCode == cKey)) return false;
        });

        // Document Ctrl + C/V
        // returns are to prevent lag on keypress
        // Unlock() makes sure the ctrl key isn't held down
        $("canvas").keydown(function (e) {
            //Paste Combos
            if (ctrlDown && (e.keyCode == vKey)) {
                lock();
                console.log("Document catch Ctrl+V");
                setTimeout(unlock, 4);
                return;
            }
            //Copy Combos
            // Copy is simpler than paste, just call the copy method.
            else if (ctrlDown && (e.keyCode == cKey)) {
                console.log("Document catch Ctrl+C");
                copyStringToClipboard(remoteHighlight);
                unlock();
                return;
            }
            //Cut
            else if (ctrlDown && (e.keyCode == xKey)) {
                console.log("Document catch Ctrl+X");
                copyStringToClipboard(remoteHighlight);
                unlock();
                return;
            }
        });

        // MAC copy paste using cmd
        // prevent default doesn't seem to work on mac
        // you have to lock unlock to stop vnc interaction
        if (mac) {
            $("canvas").keydown(function (e) {
                if (cmdDown && (e.keyCode == vKey)) {
                    lock();
                    console.log("Document catch CMD+V");
                    setTimeout(unlock, 4);
                    return;
                }
                else if (cmdDown && (e.keyCode == cKey)) {
                    console.log("Document catch CMD+C");
                    lock();
                    copyStringToClipboard(remoteHighlight);
                    unlock();
                    return;
                }
                //Cut
                // because cmd X isn't a command on remote vnc we need to emulate cut
                // it's emulated by doing a normal copy, then sending a cut key combo
                else if (cmdDown && (e.keyCode == xKey)) {
                    console.log("Document catch CMD+X");
                    lock();
                    copyStringToClipboard(remoteHighlight);
                    unlock();
                    sendCtrlX();
                    return;
                }
            });
        }
    }

    $(window).bind("paste", function (e) {
        // lambda expression added for EDGE support
        e.preventDefault ? e.preventDefault() : (e.returnValue = false);
        // vnc needs to be unlocked to paste the text into it
        unlock();
        // access the clipboard using the api
        var str = e.originalEvent.clipboardData.getData('text');
        $("#paste_box").val(str);
        console.log(str);

        // copy the client clipboard over to remote machine's clipboard
        rfb.clipboardPasteFrom(str);

        setTimeout(sendCtrlV, 5);
        $("canvas").focus();
    });

    function lock() {
        console.log("lock");
        rfb.viewOnly = 1;
    }

    function unlock() {
        if (rfb.viewOnly == 1) {
            console.log("unlock");
            rfb.viewOnly = 0;
        }
        // MAC fix, prevents sticky CMD button
        rfb.sendKey(KeyTable.XK_Meta_L, "MetaLeft");
    }

    // Simulate Key Combinations
    function sendCtrlV() {
        console.log("sending ctrl+v");
        rfb.sendKey(KeyTable.XK_Control_L, "ControlLeft", true);
        rfb.sendKey(KeyTable.XK_v, "v");
        rfb.sendKey(KeyTable.XK_Control_L, "ControlLeft", false);
    }

    function sendCtrlX() {
        console.log("sending ctrl+v");
        rfb.sendKey(KeyTable.XK_Control_L, "ControlLeft", true);
        rfb.sendKey(KeyTable.XK_x, "x");
        rfb.sendKey(KeyTable.XK_Control_L, "ControlLeft", false);
    }

    // When text is highlighted on noVNC it calls this method
    function clipboardReceive(e) {
        remoteHighlight = e.detail.text;
        $("#copy_box").val(e.detail.text);
    }

    function copyStringToClipboard(str) {
        // Create new element
        var el = document.createElement('textarea');
        // Set value (string to be copied)
        el.value = str;
        // Set non-editable to avoid focus and move outside of view
        el.setAttribute('readonly', '');
        el.style = { position: 'absolute', left: '-9999px' };
        document.body.appendChild(el);
        // Select text inside element
        el.select();
        // Copy text to clipboard
        document.execCommand('copy');
        // Remove temporary element
        document.body.removeChild(el);

        $("canvas").focus();
    }

    function pasteClicked() {
        unlock();
        var str = $("#paste_box").val();
        rfb.clipboardPasteFrom(str);
        setTimeout(sendShiftInsert, 5);
    }

    //-------------------------------------------------------------------------//
    //-------------THE CODE BELOW IS FOR THE TOP BAR CONTROLS ONLY-------------//
    //-------------------------------------------------------------------------//

    rfb.addEventListener("connect", onConnected);
    // All this code is for the top bar and fullscreen
    function onConnected() {
        // Add relevant listeners to activate the bar
        $("canvas").on('mousemove', mousemove);
        $(document.body).on('mousemove', mousemove);
        $("#fullscreen_but").on('click', fullscreenClicked);
        $("#exit_fullscreen_but").on('click', exit_fullscreenClicked);
        $("#copy_box").on('click', copy_boxClicked);

        var topBar = $("#top_bar");
        // listen for mouse move so you know when to show the topbar
        function mousemove(e) {
            // Calculate the size of the top bar (including sliding menus)
            var width = $(document.body).width();
            //additions and subtractions from bounds are for padding
            var yBound = $("#top_bar").height() + 10;
            var leftBound = (width / 3) - 5;
            var rightBound = (width - (width / 3) + 5);
            if ($("#copy_bar").hasClass('slide')) {
                leftBound = 0;
            }
            if ($("#paste_bar").hasClass('slide')) {
                rightBound = width;
            }

            // if your cursor leaves the top bar, hide it
            if (e.pageY < 10 && e.pageX >= width / 3 && e.pageX < width - (width / 3)) {
                $(topBar).addClass('show');
                $("#paste_bar").addClass('show');
                $("#copy_bar").addClass('show');
            } else if ($(topBar).hasClass('show') && (e.pageY > yBound)) {
                $(topBar).removeClass('show');
                $("#copy_bar").removeClass('show');
                $("#paste_bar").removeClass('show');

            } else if (e.pageX <= leftBound || e.pageX > rightBound) {
                $(topBar).removeClass('show');
                $("#copy_bar").removeClass('show');
                $("#paste_bar").removeClass('show');
            }
        }

        $("#copy_show").on('click', copyShowClicked);
        $("#paste_show").on('click', pasteShowClicked);
        $("#paste_but").on('click', pasteClicked);

        // show & the copy and paste menus
        function copyShowClicked() {
            if ($("#copy_bar").hasClass('slide')) {
                $("#copy_bar").removeClass('slide');
                $("#copy_bar").removeClass('visible')
            } else {
                $("#copy_bar").addClass('slide');
                $("#copy_bar").addClass('visible')
            }

        }

        function pasteShowClicked() {
            if ($("#paste_bar").hasClass('slide')) {
                $("#paste_bar").removeClass('slide');
                $("#paste_bar").removeClass('visible');
            } else {
                $("#paste_bar").addClass('slide');
                $("#paste_bar").addClass('visible');
            }
        }

        function copy_boxClicked() {
            $("#copy_box").select();
        }


        // fullscreen the window and change the text
        function fullscreenClicked() {
            var elem = document.documentElement;
            $("#fullscreen_but").hide();
            $("#exit_fullscreen_but").show();
            if (elem.requestFullscreen) {
                elem.requestFullscreen();
            } else if (elem.mozRequestFullScreen) { /* Firefox */
                elem.mozRequestFullScreen();
            } else if (elem.webkitRequestFullscreen) { /* Chrome, Safari and Opera */
                elem.webkitRequestFullscreen();
            } else if (elem.msRequestFullscreen) { /* IE/Edge */
                elem.msRequestFullscreen();
            }
        }
        // exit fullscreen and change the text
        function exit_fullscreenClicked() {
            $("#fullscreen_but").show();
            $("#exit_fullscreen_but").hide();
            if (document.exitFullscreen) {
                document.exitFullscreen();
            } else if (document.mozCancelFullScreen) { /* Firefox */
                document.mozCancelFullScreen();
            } else if (document.webkitExitFullscreen) { /* Chrome, Safari and Opera */
                document.webkitExitFullscreen();
            } else if (document.msExitFullscreen) { /* IE/Edge */
                document.msExitFullscreen();
            }
        }

    }

});
