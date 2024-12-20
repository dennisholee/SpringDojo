package contracts.customer

import org.springframework.cloud.contract.spec.Contract


	Contract.make {
	    description "200 OK - return a list of customer"
	
	    request {
	        method 'GET'
	        urlPath '/customers'
	    }
	    
	    response {
	        statuspri OK()
	        headers {
	            contentType applicationJson()
	        }
	        body ([ 
	            firstName : "John",
	            lastName : "Smith"
	        ])
	    }
	    
	}
