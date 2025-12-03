# 即梦APP

## 🚀快速开始

- 克隆仓库并切换到安卓分支

```bash
git clone https://github.com/Pineberry1/JIMENG.git
cd JIMENG
git checkout -b android_app origin/android_app
```

- 配置API_KEY（这里提供一个临时key：sk-135ccd0346344aabaeefaa497b687340）

  需要在系统环境变量里添加DASHSCOPE_API_KEY

  - **windows:**

    使用cmd:

  ```
  # 设置用户环境变量
  setx DASHSCOPE_API_KEY "sk-135ccd0346344aabaeefaa497b687340"
  
  # 或者设置系统环境变量（需要管理员权限）
  setx DASHSCOPE_API_KEY "sk-135ccd0346344aabaeefaa497b687340" /M
  ```

  	使用powershell:

  ```
  # 设置用户环境变量
  [System.Environment]::SetEnvironmentVariable('DASHSCOPE_API_KEY', 'sk-135ccd0346344aabaeefaa497b687340', 'User')
  
  # 设置系统环境变量（需要管理员权限）
  [System.Environment]::SetEnvironmentVariable('DASHSCOPE_API_KEY', 'sk-135ccd0346344aabaeefaa497b687340', 'Machine')
  ```

  - **Linux:**

  临时设置:

  ```
  export DASHSCOPE_API_KEY="sk-135ccd0346344aabaeefaa497b687340"
  ```

  永久设置:

  ```
  # 编辑 ~/.bashrc 文件
  echo 'export DASHSCOPE_API_KEY="sk-135ccd0346344aabaeefaa497b687340"' >> ~/.bashrc
  
  # 使配置立即生效
  source ~/.bashrc
  ```

  