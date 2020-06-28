server {
    listen 80;
    <#if https == true>listen 443 ssl;</#if>
    server_name ${domain};

    <#if https == true>
    ssl_certificate       ${domain}.pem;
    ssl_certificate_key   ${domain}.key;
    ssl_protocols         TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers           HIGH:!aNULL:!MD5;
    </#if>

    location / {
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP $remote_addr;

        proxy_pass http://localhost:${port?c};
    }
}
