;; Eval Buffer with `M-x eval-buffer' to register the newly created template.

(dap-register-debug-template
  "PrivacyTest1  - RunSymbolic - Debug"
  (list :name "PrivacyTest1 - RunSymbolic"
        :type "java"
        :request "launch"
        :args "-f resources/www_queries/F5.prime.rq -d http://localhost:3030/Humans/sparql -o wwww_queries_eps_1.0.txt -v true -eps 1.0"
        :cwd nil
        :stopOnEntry :json-false
        :host "localhost"
        :request "launch"
        :modulePaths []
        :classPaths nil
        :projectName nil
        :mainClasss nil))
