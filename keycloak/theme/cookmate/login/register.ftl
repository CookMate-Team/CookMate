<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>
    <#if section = "header">
        ${msg("registerTitle")}
    <#elseif section = "form">
        <script src="https://cdn.tailwindcss.com"></script>
        <script>
          tailwind.config = {
            important: true,
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
            body, .login-pf-page { background-color: #fafaf9 !important; background-image: none !important; color: #292524; font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif; }
            .login-pf-page .card-pf { background: transparent; border-top: none; box-shadow: none; padding: 0; }
            .login-pf-page .login-pf-header { display: none; }
            #kc-header { display: none; }
            a:hover { text-decoration: none !important; }
        </style>

        <div class="fixed inset-0 bg-stone-50 z-[-1]"></div>
        
        <div class="min-h-screen flex items-center justify-center -mt-8 py-12">
            <div class="max-w-md w-full bg-white rounded-xl shadow-xl p-8 border border-stone-100">
                <div class="text-center mb-8">
                    <h1 class="text-4xl font-extrabold tracking-tight bg-gradient-to-r from-amber-500 to-orange-500 text-transparent bg-clip-text">CookMate</h1>
                    <p class="mt-2 text-stone-500 font-medium">Create your account</p>
                </div>

                <form id="kc-register-form" class="space-y-4" action="${url.registrationAction}" method="post">
                    
                    <div>
                        <label for="firstName" class="block text-sm font-semibold text-stone-700 mb-1">${msg("firstName")}</label>
                        <input type="text" id="firstName" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="firstName" value="${(register.formData.firstName!'')}" autocomplete="given-name" />
                    </div>

                    <div>
                        <label for="lastName" class="block text-sm font-semibold text-stone-700 mb-1">${msg("lastName")}</label>
                        <input type="text" id="lastName" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="lastName" value="${(register.formData.lastName!'')}" autocomplete="family-name" />
                    </div>

                    <div>
                        <label for="email" class="block text-sm font-semibold text-stone-700 mb-1">${msg("email")}</label>
                        <input type="email" id="email" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="email" value="${(register.formData.email!'')}" autocomplete="email" />
                    </div>

                    <#if !realm.registrationEmailAsUsername>
                        <div>
                            <label for="username" class="block text-sm font-semibold text-stone-700 mb-1">${msg("username")}</label>
                            <input type="text" id="username" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="username" value="${(register.formData.username!'')}" autocomplete="username" />
                        </div>
                    </#if>

                    <#if passwordRequired??>
                        <div>
                            <label for="password" class="block text-sm font-semibold text-stone-700 mb-1">${msg("password")}</label>
                            <input type="password" id="password" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="password" autocomplete="new-password" />
                        </div>

                        <div>
                            <label for="password-confirm" class="block text-sm font-semibold text-stone-700 mb-1">${msg("passwordConfirm")}</label>
                            <input type="password" id="password-confirm" class="w-full px-4 py-2 rounded-xl border border-stone-200 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all" name="password-confirm" />
                        </div>
                    </#if>

                    <#if recaptchaRequired??>
                        <div class="mt-4">
                            <div class="g-recaptcha" data-sitekey="${recaptchaSiteKey}"></div>
                        </div>
                    </#if>

                    <div id="kc-form-buttons" class="mt-8 pt-2">
                        <button class="w-full bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold px-5 py-3 rounded-xl shadow-md hover:shadow-lg hover:from-amber-400 hover:to-orange-400 active:scale-[0.98] transition-all duration-200" type="submit">${msg("doRegister")}</button>
                    </div>
                </form>

                <div class="mt-6 text-center border-t border-stone-100 pt-6 flex flex-col gap-3">
                    <p class="text-sm text-stone-600">
                        <a href="${url.loginUrl}" class="font-bold text-orange-500 hover:text-orange-600 transition-colors">Back to Login</a>
                    </p>
                    <p class="text-sm text-stone-600">
                        <a href="http://localhost:5173" class="font-bold text-stone-500 hover:text-stone-800 transition-colors">Return to CookMate</a>
                    </p>
                </div>

            </div>
        </div>
    </#if>
</@layout.registrationLayout>
