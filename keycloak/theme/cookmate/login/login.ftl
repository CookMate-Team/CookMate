<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <script src="https://cdn.tailwindcss.com"></script>
        <script>
          tailwind.config = {
            theme: {
              extend: {
                colors: {
                  stone: {
                    50: '#fafaf9',
                    100: '#f5f5f4',
                    800: '#292524',
                    900: '#1c1917',
                  },
                  amber: {
                    500: '#f59e0b',
                  },
                  orange: {
                    500: '#f97316',
                    600: '#ea580c',
                  }
                }
              }
            }
          }
        </script>
        <style>
            /* Override default Keycloak styles */
            body { background-color: #fafaf9; color: #292524; font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif; }
            .login-pf-page .card-pf { background: transparent; border-top: none; box-shadow: none; padding: 0; }
            .login-pf-page .login-pf-header { display: none; }
            #kc-header { display: none; }
        </style>
        
        <div class="fixed inset-0 bg-stone-50 z-[-1]"></div>
        
        <div class="min-h-screen flex items-center justify-center -mt-16">
            <div class="max-w-md w-full bg-white rounded-xl shadow-xl p-8 border border-stone-100">
                <div class="text-center mb-8">
                    <h1 class="text-4xl font-extrabold tracking-tight bg-gradient-to-r from-amber-500 to-orange-500 text-transparent bg-clip-text">CookMate</h1>
                    <p class="mt-2 text-stone-500 font-medium">Log in to your account</p>
                </div>

                <div id="kc-form">
                  <div id="kc-form-wrapper">
                    <#if realm.password>
                        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" class="space-y-5">
                            <#if !usernameHidden??>
                                <div>
                                    <label for="username" class="block text-sm font-semibold text-stone-700 mb-1"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                                    <input tabindex="1" id="username" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off" />
                                </div>
                            </#if>

                            <div>
                                <div class="flex items-center justify-between mb-1">
                                    <label for="password" class="block text-sm font-semibold text-stone-700">${msg("password")}</label>
                                    <#if realm.resetPasswordAllowed>
                                        <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="text-sm text-orange-500 hover:text-orange-600 font-medium transition-colors">${msg("doForgotPassword")}</a>
                                    </#if>
                                </div>
                                <input tabindex="2" id="password" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="password" type="password" autocomplete="off" />
                            </div>

                            <div class="flex items-center justify-between">
                                <#if realm.rememberMe && !usernameHidden??>
                                    <div class="flex items-center">
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" class="h-4 w-4 text-orange-500 focus:ring-orange-500 border-stone-300 rounded" <#if login.rememberMe??>checked</#if>>
                                        <label for="rememberMe" class="ml-2 block text-sm text-stone-700">
                                            ${msg("rememberMe")}
                                        </label>
                                    </div>
                                </#if>
                            </div>

                            <div id="kc-form-buttons" class="mt-6">
                                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                                <button tabindex="4" class="w-full bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold px-5 py-3 rounded-xl shadow-md hover:shadow-lg hover:from-amber-400 hover:to-orange-400 active:scale-[0.98] transition-all duration-200" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
                            </div>
                        </form>
                    </#if>
                  </div>

                </div>

                <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                    <div id="kc-registration-container" class="mt-8 text-center border-t border-stone-100 pt-6 flex flex-col gap-3">
                        <p class="text-sm text-stone-600">
                            ${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}" class="font-bold text-orange-500 hover:text-orange-600 transition-colors">${msg("doRegister")}</a>
                        </p>
                        <p class="text-sm text-stone-600">
                            <a href="http://localhost:5173" class="font-bold text-stone-500 hover:text-stone-800 transition-colors">Return to CookMate</a>
                        </p>
                    </div>
                <#else>
                    <div class="mt-8 text-center border-t border-stone-100 pt-6">
                        <p class="text-sm text-stone-600">
                            <a href="http://localhost:5173" class="font-bold text-stone-500 hover:text-stone-800 transition-colors">Return to CookMate</a>
                        </p>
                    </div>
                </#if>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
