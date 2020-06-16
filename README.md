# java2json

A simple plugin for converting Java bean to JSON in IntelliJ IDEA

When you post json request using Postman :)

## Screenshot

![](https://raw.githubusercontent.com/linsage/java2json/master/screenshot/java2json.gif)

https://plugins.jetbrains.com/plugin/10336-java-bean-to-json

## Change Log
- v1.0
    - First revision
- v1.0.1
    - update plugin info
- v1.0.2
    - add doc comment
- v1.0.3
    - support time class type
- v1.0.4
    - build IntelliJ IDEA 192.* due to Java functionality
- v1.0.5
    - support enum

- v2.0
    - Second revision by kevin
- v2.0.1    
    - add yapi json schema support
- v2.0.2
    - support Unicode 转 中文
    
## Todo List
- 字段的注解处理

## Reference
https://github.com/FurionCS/PojoToJson

## Support
![](https://note-1256162930.picgz.myqcloud.com/zo1mm.jpg)

## How to build

- 配置
1) 在IDEA 添加 plugin SDK 
2）重启IDEA

- 打包
选择 Build | Prepare Plugin Module ‘module name’ for Deployment 来打包插件：
jar包会保存到：/Users/kevin/Documents/code/github/java2json/.idea/java2json.jar