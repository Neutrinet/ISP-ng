"use strict";

// disclaimer: this script is written by someone who's not particularly good
// or interested in javascript. anyone willing to port this to something decent
// like angular.js, please do so

function VPN() {
    var vpn = this;

    this.registration = {};
    this.endpoint = '/';
    // temporary, for primitive backoffice
    this.user = {};

    this.createUser = function (username) {

    };

    this.handleIfError = function(response) {
        if (response === undefined) {
            var errorBlock = $('<div class="alert alert-danger" role="alert">');

            app.content.empty();
            errorBlock.append($('<h2>').text('An error ocurred'));
            errorBlock.append($('<p>').text('Our backend did not return a response. Please try again.'));
            app.content.append(errorBlock);
            app.preloader.hide();
            app.content.fadeIn();

            return true;
        }

        if (response.errorKey !== undefined) {
            var errorBlock = $('<div class="alert alert-danger" role="alert">');

            //app.content.empty();
            errorBlock.prepend($('<br>'));
            errorBlock.prepend($('<p>').text(response.errorKey));
            errorBlock.prepend($('<p>').text(response.message));
            errorBlock.prepend($('<h2>').text('An error ocurred'));

            app.content.prepend(errorBlock);

            app.preloader.hide();
            app.content.fadeIn();

            return true;
        }

        return false;
    };

    this.confirm = function() {
        if (vpn.registration != undefined) {
            app.content.hide();
            app.preloader.show();
            $.ajax(vpn.endpoint + 'api/reg/commit', {
                data: JSON.stringify(vpn.registration),
                type: 'POST',
                contentType: 'application/json',
                dataType: 'json',
                success: function(response, status, xhr) {
                    $('#content').load('done.html', function() {
                        //activate starz
                        var mq = window.matchMedia('only screen and (min-device-width: 800px)');
                        if (mq.matches) {
                            $.getScript('js/starz.js', function() {
                                new StarField('starz').render(50, 1);
                            });
                        }
                        $('#email').text(vpn.registration.user.email);
                        app.preloader.hide();
                        app.content.fadeIn();
                    });
                }});
        }
    };

    this.validateKey = function(email, key) {
        app.content.hide();
        app.preloader.show();
        $.ajax(vpn.endpoint + 'api/reg/validateKey', {
            data: JSON.stringify({'email': email, 'key': key}),
            type: 'POST',
            contentType: 'application/json',
            dataType: 'json',
            success: function(response, status, xhr) {
                if (vpn.handleIfError(response))
                    return;
                vpn.registration = response;
                app.preloader.hide();
                app.content.load('password.html', app.unlocked);
            }});
    };
}

function App() {
    var self = this;
    this.preloader = $('#preloader');
    this.content = $('#content');
    this.token = "";
    this.vpn = new VPN();
    this.urlParams = {};

    this.run = function() {

        if (!window.location.origin)
            window.location.origin = window.location.protocol + "//" + window.location.host;

        self.vpn.endpoint = window.location.origin + '/';

        //setup
        $.ajaxSetup({
            error: self.ajaxError
        });

        window.onpopstate = self.parseQueryString;

        //load content
        self.content.hide();

        //check for re-entry
        self.parseQueryString();

        $(document).ready(function() {
            if (self.urlParams["flow"] != undefined) {
                if (self.handleFlow())
                    return;
            }

            $('#content').load('start.html', function() {

                $('#login-link').click(function(e){
                    e.preventDefault();
                    e.stopImmediatePropagation();

                    app.content.fadeOut();
                    app.preloader.fadeIn();
                    $('#content').load('login.html', self.login);
                });

                self.preloader.fadeOut();
                self.content.fadeIn();

                $('form .btn-primary').click(function(event) {
                    self.vpn.validateKey($('#signup-email').val(), $('#signup-key').val());
                });
            });
        });
    };

    this.handleFlow = function() {
        self.vpn.registration.id = self.urlParams['id'];

        if (self.vpn.registration.id == undefined)
            return false;

        $.ajax(self.vpn.endpoint + 'api/reg/' + self.vpn.registration.id, {
            type: 'GET',
            contentType: 'application/json',
            dataType: 'json',
            success: function (response, status, xhr) {
                self.vpn.registration = response;
                if (self[self.urlParams['flow']] == undefined) {
                    self.ajaxError(null, null, 'Illegal flow');
                } else {
                    // execute flow handler
                    document.cookie = 'Registration-ID=' + self.vpn.registration.id;
                    return self[self.urlParams["flow"]]();
                }
            }
        });

        return false;
    };

    this.setupVPN = function() {
        $('#content').load('config.html', function() {
            self.preloader.hide();
            self.content.fadeIn();
            var platform = "";
            if (navigator.userAgent.indexOf("Win") != -1)
                platform = "windows";
            if (navigator.userAgent.indexOf("Mac") != -1)
                platform = "osx";
            if (navigator.userAgent.indexOf("X11") != -1)
                platform = "unix";
            if (navigator.userAgent.indexOf("Linux") != -1)
                platform = "linux";
            if (navigator.userAgent.indexOf("BSD") != -1)
                platform = "linux";

            $('div.platform-details').each(function(e) {
                $(this).hide();
            });

            $('div#' + platform).show();

            $('div#' + platform + ' .download-button').click(function() {
                // sadly, jQuery have their nickers in a twist when it comes
                // to XHR2 support (which allows for Blob return content types) in $.ajax
                // Ugh.

                var xhr = new XMLHttpRequest();
                xhr.open('POST', self.vpn.endpoint + 'api/user/' + self.vpn.registration.user.id + '/config', true);
                xhr.responseType = 'blob';
                xhr.onload = function(e) {
                    if (this.status == 200) {
                        $.getScript('js/FileSaver.js', function() {
                            var blob = new Blob([xhr.response], {type: 'application/zip; charset=utf-8'});
                            if (platform == "osx")
                                saveAs(blob, "neutrinet.tblk.zip");
                            else
                                saveAs(blob, "neutrinet.zip");
                        });
                    }
                };
                xhr.onerror = app.ajaxError;
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send(JSON.stringify({regId: self.vpn.registration.id, platform: platform}));
            });
        });

        return true;
    };

    this.emailDone = this.setupVPN;

    this.eIdDone = function() {
        $.ajax(self.vpn.endpoint + 'api/reg/' + self.vpn.registration.id, {
            type: 'GET',
            contentType: 'application/json',
            dataType: 'json',
            success: function(response, status, xhr) {
                if (response == undefined) {
                    window.location = window.location.origin;
                    return;
                }
                self.vpn.registration = response;
                app.content.hide();
                app.preloader.fadeIn();
                $('#content').load('review.html', self.review);
            }
        });
    };

    this.keypairSelect = function() {
        app.preloader.hide();
        app.content.fadeIn();

        $('#use-eid').click(function() {
            app.content.hide();
            app.preloader.fadeIn();
            app.content.load('review.html', self.review);
        });

        $('#use-csr').click(function() {
            app.content.hide();
            app.preloader.fadeIn();
            app.content.load('cert.html', self.useCSR);
        });
    };

    this.useCSR = function() {
        var scripts = ['js/asn1js/asn1.js', 'js/asn1js/base64.js', 'js/asn1js/oids.js'];
        var numloaded = 0;

        // super awesome dependency loader
        for (var i = 0; i < scripts.length; i++) {
            $.getScript(scripts[i], function() {
                numloaded++;

                if (numloaded === scripts.length) {
                    app.preloader.hide();
                    app.content.fadeIn();

                    var reHex = /^\s*(?:[0-9A-Fa-f][0-9A-Fa-f]\s*)+$/;
                    var feedback = $('#feedback');

                    $('#csr').on('input', function(e) {
                        feedback.hide();
                        feedback.removeClass();
                        var pem = $('#csr').val();

                        try {
                            var der = reHex.test(pem) ? Hex.decode(pem) : Base64.unarmor(pem);
                            var asn1 = ASN1.decode(der);
                            $('#get-cert').addClass('btn-primary')
                            $('#get-cert').prop("disabled", false);
                        } catch (e) {
                            feedback.text("The CSR you entered is invalid. Please make sure you've " +
                                "correctly followed the instructions above and pasted the whole CSR. The parser " +
                                "expects a Base64-armored PKCS10 instance.");
                            feedback.fadeIn();
                            $('#get-cert').removeClass('btn-primary')
                            $('#get-cert').prop("disabled", true);
                        }
                    });

                    $('#get-cert').click(function() {
                        // set cookie to avoid auth dialog
                        document.cookie = 'Registration-ID=' + self.vpn.registration.id;

                        $.ajax(self.vpn.endpoint + 'api/user/' + self.vpn.registration.user.id + '/cert/new', {
                            data: $('#csr').val(),
                            type: 'PUT',
                            contentType: 'application/json',
                            dataType: 'json',
                            success: function(response, status, xhr) {
                                $('#error').empty();
                                app.content.hide();
                                app.preloader.fadeIn();
                                $('#content').load('review.html', self.review);
                            },
                            error: function(response, status, xhr) {
                                $('#error').append($('<b>').text(response.errorKey));
                                $('#error').append($('<p>').text(response.message));
                            }
                        });
                    });
                }
            });
        }
    };

    this.review = function() {
        $.getScript('js/renderjson.js', function() {
            app.preloader.hide();
            $('#ip-address input[type="checkbox"]').bootstrapSwitch();
            $('#ip-address input[type="checkbox"]').on('switchChange.bootstrapSwitch', self.requestIP);
            $('#ip6-address-request').bootstrapSwitch('state', true);
            $('#ip6-address-request').bootstrapSwitch('readonly', true);
            $('#user-details').append(renderjson(self.vpn.registration.user));
            $('#confirm').click(self.vpn.confirm);
            app.content.fadeIn();
        });
    };

    this.requestIP = function(event, state) {
        var version = event.currentTarget.id.charAt(2);
        $(event.currentTarget).bootstrapSwitch('indeterminate', true);

        if (state == true)
            $.ajax(self.vpn.endpoint + 'api/address/lease/0', {
                data: JSON.stringify({
                    user: self.vpn.registration.user.id,
                    version: parseInt(version, 10)
                }),
                type: 'PUT',
                contentType: 'application/json',
                dataType: 'json',
                success: function(response, status, xhr) {
                    self.vpn.handleIfError(response);
                    $(event.currentTarget).bootstrapSwitch('indeterminate', false);
                    $(event.currentTarget).bootstrapSwitch('state', true, true);
                    $('#ip' + version + '-address').text(response.address + "/" + response.netmask);
                    self.vpn.registration["ipv" + version + "Id"] = response.id;
                },
                error: function(response, status, xhr) {
                    $(event.currentTarget).bootstrapSwitch('indeterminate', false);
                    $(event.currentTarget).bootstrapSwitch('state', false, true);
                    $('#ip' + version + '-address').text("No IPv" + version + " available");
                }});
    };

    this.unlocked = function() {
        app.preloader.hide();
        app.content.fadeIn();

        $('#password').keyup(self.validatePassword);
        $('#password-verify').keyup(self.validatePassword);
        $('#password-done').click(function() {
            $.ajax(self.vpn.endpoint + 'api/reg/enterPassword', {
                data: JSON.stringify({
                    id: self.vpn.registration.id,
                    password: $('#password').val()
                }),
                type: 'POST',
                contentType: 'application/json',
                dataType: 'json',
                success: function(response, status, xhr) {
                    self.vpn.registration['user'] = response;
                    app.content.hide();
                    app.preloader.fadeIn();
                    $('#content').load('eid.html', self.useEIDIdentification);
                }});
        });
    };

    this.parseQueryString = function() {
        var match,
                pl = /\+/g, // Regex for replacing addition symbol with a space
                search = /([^&=]+)=?([^&]*)/g,
                decode = function(s) {
                    return decodeURIComponent(s.replace(pl, " "));
                },
                query = window.location.search.substring(1);

        self.urlParams = {};
        while (match = search.exec(query))
            self.urlParams[decode(match[1])] = decode(match[2]);
    };

    this.validatePassword = function() {
        var pwd = $('#password').val();
        var verify = $('#password-verify').val();

        console.log('l' + pwd.length + " " + (pwd === verify ? "true" : "false"));

        if (pwd.length < 6) {
            $('#password').parent().addClass("has-error")
            $('#alert-password').removeClass("hide")
        } else {
            $('#password').parent().removeClass("has-error")
            $('#alert-password').addClass("hide")
        }

        if (pwd !== verify) {
            $('#password-verify').parent().addClass("has-error")
            $('#alert-password-verify').removeClass("hide")
        } else {
            $('#password-verify').parent().removeClass("has-error")
            $('#alert-password-verify').addClass("hide")
        }

        if (pwd.length < 6 || pwd !== verify) {
            $('#password-done').attr('disabled', '');
        } else {
            $('#password-done').removeAttr('disabled');
        }

    };

    this.login = function() {
        self.preloader.fadeOut();
        self.content.fadeIn();

        $('#login').click(self.handleLogin);
    };

    this.handleLogin = function(e) {
        e.preventDefault();
        e.stopImmediatePropagation();

        var user = $('#username').val();
        var password = $('#password').val();

        $.ajax(self.vpn.endpoint + 'api/user/login', {
            data: JSON.stringify({user: user, password: password}),
            type: 'POST',
            contentType: 'application/json',
            username: user,
            password: password,
            dataType: 'json',
            success: function (response, status, xhr) {
                app.content.hide();
                app.preloader.show();
                self.vpn.user = response;
                $('#content').load('user.html', self.userMgmt);
            }
        })
    };

    this.userMgmt = function() {

    };

    this.manualReg = function () {
        app.preloader.hide();
        app.content.show();

        $('#reg').click(function () {
            var data = {};

            var fields = $('#reg-form').children('input[type=text]');
            for (var i in fields) {
                var field = fields[i];
                
                if (typeof field !== 'object') continue;
                
                data[field.id] = $(field).val();
            }
            
            data['country'] = $('#country').val();
            data['id'] = self.vpn.registration.id;

            $.ajax(self.vpn.endpoint + 'api/reg/manual', {
                data: JSON.stringify(data),
                type: 'POST',
                contentType: 'application/json',
                dataType: 'json',
                success: function (response, status, xhr) {
                    self.vpn.registration = response;
                    app.content.hide();
                    app.preloader.fadeIn();
                    $('#content').load('keypair.html', self.keypairSelect);
                }
            });
        });
    };

    this.ajaxError = function(xhr, errorType, exception) {
        var errorMessage = exception || xhr.statusText;

        app.content.html('<h2>Error</h2><p>' + errorMessage + '</p>');
        app.preloader.hide();
        app.content.fadeIn();
    };

    this.useEIDIdentification = function() {
        app.preloader.hide();
        app.content.fadeIn();

        $('#applet-container').click(function () {
            $.getScript('js/deployJava.js', function () {
                // ugly hack to allow vanilla deployJava.js version
                // http://stackoverflow.com/questions/13517790/using-deployjava-runapplet-to-target-specific-element
                function docWriteWrapper(jq, func) {
                    var oldwrite = document.write, content = '';
                    document.write = function (text) {
                        content += text;
                    };
                    func();
                    document.write = oldwrite;
                    jq.html(content);
                }

                var attributes = {
                    alt: 'eID applet',
                    code: 'be.fedict.eid.applet.Applet.class',
                    archive: 'objects/eid-applet-package-1.2.0-SNAPSHOT.jar',
                    width: 700,
                    height: 300
                };
                var parameters = {
                    TargetPage: app.vpn.endpoint + 'flow/attach-eid/' + self.vpn.registration.id,
                    AppletService: app.vpn.endpoint + 'applet',
                    BackgroundColor: '#ffffff',
                    codebase_lookup: 'false',
                    cache_option: 'no'
                };
                var version = '1.6';

                // fix for Mac OS X 64 bit
                var javaArgs = '';
                if (navigator.userAgent.indexOf('Mac OS X 10_6') !== -1
                    || navigator.userAgent.indexOf('Mac OS X 10.6') !== -1) {
                    javaArgs += '-d32';
                }
                parameters.java_arguments = javaArgs;
                // fix for IE 7/8
                var browser = deployJava.getBrowser();
                if (browser === 'MSIE') {
                    version = '1.6.0_27';
                }

                app.preloader.hide();
                app.content.fadeIn();

                docWriteWrapper($('#applet-container'), function () {
                    deployJava.runApplet(attributes, parameters, version);
                });
            });
        });

        $('#manual-reg').click(function () {
            app.content.hide();
            app.preloader.show();
            app.content.load('reg-form.html', self.manualReg);
        });
    };
}

var app = new App();
app.run();
