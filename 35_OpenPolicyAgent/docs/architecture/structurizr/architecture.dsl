workspace "Authorization Architecture" "An authorization architecture using OPA as a Policy Decision Point." {

    !identifiers hierarchical

    model {
        // Define People and Systems
        consumer = person "Consumer" "A user or service that requires access to a protected resource."
        authz_system = softwareSystem "Authorization System" "Manages authentication and authorization for services." {
            // Define Containers within the Authorization System
            authz_server = container "Authz Server (PEP)" "The Policy Enforcement Point, a Spring Boot application that intercepts requests and queries OPA." "Spring Boot"
            opa_app = container "OPA Application (PDP)" "A custom-built Spring Boot application hosting the OPA engine and policies." "Spring Boot, OPA, Rego"
            // Define a placeholder for the actual data store if needed, though OPA often uses in-memory data
            policy_store = container "Policy Store" "A database or storage mechanism for policies and potentially cached data." "Git repo/Database"


            // Define Relationships
            consumer -> authz_server "Requests access to protected resources" "HTTPS"
            authz_server -> opa_app "Requests authorization decision (allow/deny)" "HTTPS/gRPC"
            opa_app -> policy_store "Loads policies and data from" "HTTPS/Internal API"
            // policy_store <- "Managed by" consumer "via separate management interface" "HTTPS"
        }
    }

    views {
        properties {
            "structurizr.metadata" "false"
        }

        // System Context Diagram
        systemContext authz_system "SystemContext" "The system context diagram for the authorization architecture." {
            include *
            autolayout lr
        }

        // Container Diagram
        container authz_system "ContainerDiagram" "The container diagram highlighting the PEP and PDP components." {
            include *
            autolayout lr
        }

        // Styles (optional, but good practice)
        styles {
            element "Person" {
                shape Box
                background "#0866ff"
                color "#ffffff"
            }
            element "SoftwareSystem" {
                background "#1168bd"
                color "#ffffff"
            }
            element "Container" {
                background "#438dd5"
                color "#ffffff"
            }
            element "Policy Store" {
                shape Cylinder
                background "#9a9a9a"
                color "#ffffff"
            }
        }
    }
}