# --- Mixed GET and POST Requests ---

# GET Request 1: Basic with random param and UUID auth
curl -X GET "http://localhost:8080/servlet/service?param1=$((RANDOM % 100))&userId=user-$(uuidgen | head -c 8)" \
     -H "Auth: Bearer $(uuidgen)" \
     -H "Accept: application/json"

# POST Request 1: Basic JSON payload with random data
curl -X POST "http://localhost:8080/servlet/service?param1=create_entry_$(date +%s)" \
     -H "Auth: Bearer $(uuidgen)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": "data_$(openssl rand -hex 10)",
           "field2": $((RANDOM % 1000 + 1)),
           "status": "pending"
         }'

# GET Request 2: Different param and simple token auth
curl -X GET "http://localhost:8080/servlet/service?param1=item_$(openssl rand -hex 4)&sessionId=sess-$(date +%s)" \
     -H "Auth: SimpleToken-$(openssl rand -hex 16)" \
     -H "User-Agent: MyApp/1.0"

# POST Request 2: Different header and boolean/string payload
curl -X POST "http://localhost:8080/servlet/service?param1=update_record_$(date +%s%N)" \
     -H "Auth: Session-$(date +%s%N)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": "updated_value_$(openssl rand -hex 8)",
           "field2": true,
           "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
         }'

# GET Request 3: Boolean param and Base64 auth
curl -X GET "http://localhost:8080/servlet/service?param1=true&sortBy=name&limit=$((RANDOM % 20 + 5))" \
     -H "Auth: Basic $(echo -n 'user:pass' | base64)" \
     -H "X-Request-ID: $(uuidgen)"

# POST Request 3: Custom user header and nested JSON
curl -X POST "http://localhost:8080/servlet/service?param1=add_user_$(uuidgen | head -c 8)" \
     -H "Auth: User-1234-Auth" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": { "name": "John Doe", "email": "john.doe_$(openssl rand -hex 4)@example.com" },
           "field2": "new_user_entry_$(date +%s)",
           "role": "guest"
         }'

# GET Request 4: Numeric param with range and custom header
curl -X GET "http://localhost:8080/servlet/service?param1=$((RANDOM % 900 + 100))&category=electronics_$(openssl rand -hex 3)" \
     -H "Auth: Token_$(openssl rand -hex 10)" \
     -H "Cache-Control: no-cache"

# POST Request 4: API Key in header and array in payload
curl -X POST "http://localhost:8080/servlet/service?param1=submit_batch_$(date +%s)" \
     -H "Auth: ApiKey-$(uuidgen | head -c 12)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": ["item_A_$(openssl rand -hex 2)", "item_B_$(openssl rand -hex 2)"],
           "field2": "batch_$(date +%s)",
           "count": $((RANDOM % 10 + 5))
         }'

# GET Request 5: String param with spaces (URL encoded) and API key
curl -X GET "http://localhost:8080/servlet/service?param1=search+query+$(date +%N)_$(openssl rand -hex 4)" \
     -H "Auth: ApiKey $(openssl rand -base64 24)" \
     -H "Origin: http://mywebapp.com"

# POST Request 5: Random data and custom request ID
curl -X POST "http://localhost:8080/servlet/service?param1=log_event_$(uuidgen | head -c 6)" \
     -H "Auth: $(uuidgen)_log_token" \
     -H "Content-Type: application/json" \
     -H "X-Request-ID: $(uuidgen)" \
     -d '{
           "field1": "event_type_static_$(openssl rand -hex 3)",
           "field2": "description for event $(date +%s)_$(openssl rand -hex 5)",
           "severity": "INFO"
         }'

# GET Request 6: Mixed params and user-agent string
curl -X GET "http://localhost:8080/servlet/service?param1=gamma_$(openssl rand -hex 3)&page=$((RANDOM % 5 + 1))&pageSize=25" \
     -H "Auth: $(uuidgen)-session-token" \
     -H "User-Agent: Mozilla/5.0 (compatible; MyCrawler/1.1; +http://mycrawler.com)"

# POST Request 6: JWT-like token and float value in payload
curl -X POST "http://localhost:8080/servlet/service?param1=process_order_$(openssl rand -hex 6)" \
     -H "Auth: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.random.signature_$(openssl rand -hex 5)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": "order_$(openssl rand -hex 5)",
           "field2": $((RANDOM % 90 + 10)).$((RANDOM % 99 + 1)),
           "currency": "USD"
         }'

# GET Request 7: Random enum-like param and version header
curl -X GET "http://localhost:8080/servlet/service?param1=type_$(openssl rand -hex 4)&version_tag=$(openssl rand -hex 2)" \
     -H "Auth: FixedAuthKey123" \
     -H "X-API-Version: 1.2"

# POST Request 7: No Auth header, custom query param and simple text payload
curl -X POST "http://localhost:8080/servlet/service?param1=send_message_$(date +%s)&recipient=STATICRE_$(openssl rand -hex 3)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": "Hello from $(hostname)_$(openssl rand -hex 4)",
           "field2": "Random message payload $(uuidgen)"
         }'

# GET Request 8: Epoch timestamp as param and custom correlation ID
curl -X GET "http://localhost:8080/servlet/service?param1=$(date +%s)_$(openssl rand -hex 3)" \
     -H "Auth: ClientID_75_$(openssl rand -hex 2)" \
     -H "X-Correlation-ID: $(uuidgen)"

# POST Request 8: Basic auth and status update
curl -X POST "http://localhost:8080/servlet/service?param1=change_status_$(uuidgen | head -c 5)" \
     -H "Auth: Basic $(echo -n 'admin:securepass' | base64)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": "entity_42_$(openssl rand -hex 3)",
           "field2": "COMPLETED",
           "message": "Task finished successfully_$(date +%s)"
         }'

# GET Request 9: Short random string param and simple bearer token
curl -X GET "http://localhost:8080/servlet/service?param1=$(openssl rand -hex 4)&filter=$(openssl rand -hex 3)" \
     -H "Auth: Bearer MYSECRETTOKEN123ABC" \
     -H "Referer: http://previouspage.com"

# POST Request 9: Conditional logic via query param and random object fields
curl -X POST "http://localhost:8080/servlet/service?param1=conditional_action_$(date +%s)&action_type=$(openssl rand -hex 4)" \
     -H "Auth: Agent_$(openssl rand -hex 10)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": { "id": $((RANDOM % 1000)), "name": "Object_ABCDEF_$(openssl rand -hex 3)" },
           "field2": $((RANDOM % 2 == 0)),
           "reason": "Automated trigger_$(uuidgen | head -c 4)"
         }'

# GET Request 10: Specific ID param and digest auth placeholder
curl -X GET "http://localhost:8080/servlet/service?param1=product_XYZ_33_$(openssl rand -hex 2)&status=active" \
     -H "Auth: Digest username='testuser_$(openssl rand -hex 2)', realm='myrealm'" \
     -H "Accept-Language: en-US,en;q=0.9"

# POST Request 10: Multi-part value for field1 and large random number for field2
curl -X POST "http://localhost:8080/servlet/service?param1=submit_form_$(uuidgen | head -c 7)" \
     -H "Auth: AppServiceToken-$(date +%s)" \
     -H "Content-Type: application/json" \
     -d '{
           "field1": "part_A_$(openssl rand -hex 4)-part_B_$(openssl rand -hex 4)",
           "field2": $((RANDOM % 900000 + 100000)),
           "metadata": { "source": "API_$(openssl rand -hex 2)", "version": "2.0" }
         }'
