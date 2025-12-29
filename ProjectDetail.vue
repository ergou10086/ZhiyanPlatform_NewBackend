<template>
    <div class="project-detail-container" :class="{ 'is-admin': isProjectManager }">
    <div v-if="isOperationLoading" class="operation-loading-overlay">
      <div class="operation-loading-content">
        <div class="loading-spinner"></div>
        <p class="loading-text">{{ operationLoadingText || '处理中...' }}</p>
      </div>
    </div>
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">正在加载项目详情...</p>
    </div>
    <!-- 主要内容 -->
    <div v-else>
    <!-- 顶部导航栏 -->
    <div class="top-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack" aria-label="返回">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M19 12H5M12 19L5 12L12 5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="page-title">项目详情</span>
      </div>
    </div>
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="!project" class="loading-container">
        <div class="loading-spinner"></div>
        <p>正在加载项目详情...</p>
      </div>
      <!-- 项目详情内容 -->
      <div v-if="project">
      <!-- 项目信息卡片 -->
      <div class="project-card">
        <!-- 顶部：标题和操作按钮 -->
        <div class="project-header-top">
          <h1 class="project-title">
            {{ project.title }}
          </h1>
          <div class="project-actions" v-if="canManageProject">
            <button class="btn secondary" @click="editProject">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M18.5 2.5C18.8978 2.10218 19.4374 1.87868 20 1.87868C20.5626 1.87868 21.1022 2.10218 21.5 2.5C21.8978 2.89782 22.1213 3.43739 22.1213 4C22.1213 4.56261 21.8978 5.10218 21.5 5.5L12 15L8 16L9 12L18.5 2.5Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              编辑项目
            </button>
            <!-- 删除项目按钮只对OWNER显示 -->
            <button v-if="canOperateAsOwner" class="btn btn-danger" @click="deleteProject">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3 6H5H21M8 6V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V6M19 6V20C19 20.5304 18.7893 21.0391 18.4142 21.4142C18.0391 21.7893 17.5304 22 17 22H7C6.46957 22 5.96086 21.7893 5.58579 21.4142C5.21071 21.0391 5 20.5304 5 20V6H19Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              删除项目
            </button>
          </div>
        </div>
        <!-- 主体内容：项目信息和图片并排 -->
        <div class="project-body">
          <div class="project-info">
            <!-- 第一行：项目简介和项目周期 -->
            <div class="project-meta-top">
              <div class="meta-item">
                <span class="meta-label">项目简介：</span>
                <span class="meta-value">{{ project.description }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">项目周期：</span>
                <span class="meta-value">{{ project.period }}</span>
              </div>
            </div>
            <!-- 第二行：当前状态和负责人（往下放，填充空白） -->
            <div class="project-meta-bottom">
              <div class="meta-item">
                <span class="meta-label">当前状态：</span>
                <span class="status-badge" :class="statusClass(project.status)">{{ getStatusDisplay(project.status) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">负责人：</span>
                <span class="meta-value">{{ project.manager }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">任务数量：</span>
                <span class="meta-value">{{ taskCount }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">参与人数：</span>
                <span class="meta-value">{{ participantCount }}</span>
              </div>
              <div class="meta-item" v-if="project.tags && project.tags.length > 0">
                <span class="meta-label">项目标签：</span>
                <div class="tags-container">
                  <span v-for="(tag, index) in project.tags" :key="index" class="tag">{{ tag }}</span>
                </div>
              </div>
            </div>
            <!-- 信息区操作按钮 -->
            <div class="info-actions">
              <button
                class="btn primary"
                @click="goToProjectKnowledge"
              >
                知识库
              </button>
              <button
                class="btn"
                @click="goToAIAssistant"
              >
                AI实验分析助手
              </button>
              <button
                class="btn"
                @click="goToProjectDashboard"
              >
                仪表盘
              </button>
              <button
                class="btn"
                @click="goToOperationLog"
              >
                操作日志
              </button>
            </div>
          </div>
          <!-- 项目图片区域 -->
          <div class="project-image-section">
            <div class="project-image-container">
              <img 
                v-if="project.image || project.imageUrl" 
                :src="project.image || project.imageUrl" 
                alt="项目图片" 
                class="project-image"
                @load="onImageLoad"
                @error="onImageError"
              />
              <div v-else class="project-image-placeholder">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M21 19V5C21 3.89543 20.1046 3 19 3H5C3.89543 3 3 3.89543 3 5V19C3 20.1046 3.89543 21 5 21H19C20.1046 21 21 20.1046 21 19Z" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M8 10C9.10457 10 10 9.10457 10 8C10 6.89543 9.10457 6 8 6C6.89543 6 6 6.89543 6 8C6 9.10457 6.89543 10 8 10Z" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M3 18L8 13L14 19M14 12L21 3" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <!-- 项目管理员可以上传图片 -->
              <div v-if="canManageProject" class="project-image-overlay" @click="triggerImageUpload">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 5V19M5 12H19" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>上传图片</span>
              </div>
            </div>
            <!-- 隐藏的图片上传输入 -->
            <input 
              ref="projectImageUpload" 
              type="file" 
              accept="image/*" 
              @change="handleProjectImageUpload" 
              style="display: none"
            />
          </div>
        </div>
      </div>
      <!-- 任务列表 -->
      <div class="section-card">
        <div class="section-header">
          <h2 class="section-title">任务列表</h2>
          <div class="section-actions">
            <div class="dropdown" @click.stop="toggleTaskTypeDropdown">
              <button class="btn secondary">
                <span>所有类型</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <ul class="dropdown-menu" v-if="taskTypeOpen">
                <li class="dropdown-item" :class="{ active: selectedTaskType === '' }" @click="selectTaskType('')">所有类型</li>
                <li class="dropdown-item" :class="{ active: selectedTaskType === '高' }" @click="selectTaskType('高')">高优先级</li>
                <li class="dropdown-item" :class="{ active: selectedTaskType === '中' }" @click="selectTaskType('中')">中优先级</li>
                <li class="dropdown-item" :class="{ active: selectedTaskType === '低' }" @click="selectTaskType('低')">低优先级</li>
              </ul>
            </div>
            <button
              v-if="canManageProject"
              class="btn primary"
              @click="createTask"
              :disabled="isArchived"
              :title="isArchived ? '项目已归档，仅支持查看，不能新建任务' : '新建任务'"
            >新建任务</button>
          </div>
        </div>
        <!-- 空状态提示 -->
        <div v-if="filteredTasks.length === 0" class="task-empty-state">
          <div class="empty-state-content">
            <div class="empty-state-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 11H15M9 15H15M17 21H7C5.89543 21 5 20.1046 5 19V5C5 3.89543 5.89543 3 7 3H17C18.1046 3 19 3.89543 19 5V19C19 20.1046 18.1046 21 17 21Z" stroke="#6c757d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <h3 class="empty-state-title">暂无任务</h3>
            <p class="empty-state-message">
              点击右侧<span class="highlight-text">"新建任务"</span>按钮来创建第一个任务
            </p>
          </div>
        </div>
        <!-- 任务列表横向滚动 -->
        <div v-else class="task-grid">
          <div v-for="task in filteredTasks" :key="task.id" class="task-card" @click="openTaskDetailModal(task)">
            <div class="task-header" @click.stop>
            <div class="task-priority" :class="priorityClass(task.priority)">{{ task.priority }}</div>
              <div class="task-actions" v-if="canManageProject">
                <div class="task-status-dropdown">
                  <button 
                    class="task-status-btn" 
                    :class="[statusClass(task.status), { 'disabled': isArchived || (task.status === '待接取' && (!task.assignee_name || task.assignee_name === '')) }]"
                    @click="toggleTaskStatusDropdown(task)" 
                    :title="isArchived ? '项目已归档，仅支持查看，不能更改任务状态' : (task.status === '待接取' && (!task.assignee_name || task.assignee_name === '') ? '任务未被接取，无法修改状态' : '更改状态')"
                    :disabled="isArchived || (task.status === '待接取' && (!task.assignee_name || task.assignee_name === ''))">
                    {{ task.status }}
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </button>
                  <div class="task-status-menu" v-if="task.showStatusMenu">
                    <!-- 移除"待接取"选项，用户不应该手动将任务改回待接取状态 -->
                    <button @click="changeTaskStatus(task, '进行中')" class="status-option" :class="{ active: task.status === '进行中' }">进行中</button>
                    <button @click="changeTaskStatus(task, '阻塞')" class="status-option" :class="{ active: task.status === '阻塞' }">阻塞</button>
                    <button @click="changeTaskStatus(task, '待审核')" class="status-option" :class="{ active: task.status === '待审核' }">待审核</button>
                    <button @click="changeTaskStatus(task, '完成')" class="status-option" :class="{ active: task.status === '完成' }">完成</button>
                  </div>
                </div>
                <button
                  class="task-assign-manager-btn"
                  @click="openAssignTaskModal(task)"
                  :disabled="isArchived"
                  :title="isArchived ? '项目已归档，仅支持查看，不能分配任务' : '分配任务'"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M16 21V19C16 17.9391 15.5786 16.9217 14.8284 16.1716C14.0783 15.4214 13.0609 15 12 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M20 8V14M23 11H17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <button
                  class="task-edit-btn"
                  @click="editTask(task)"
                  :disabled="isArchived"
                  :title="isArchived ? '项目已归档，仅支持查看，不能编辑任务' : '编辑任务'"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M18.5 2.5C18.8978 2.10218 19.4374 1.87868 20 1.87868C20.5626 1.87868 21.1022 2.10218 21.5 2.5C21.8978 2.89782 22.1213 3.43739 22.1213 4C22.1213 4.56261 21.8978 5.10218 21.5 5.5L12 15L8 16L9 12L18.5 2.5Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <button
                  class="task-delete-btn"
                  @click="deleteTask(task.id)"
                  :disabled="isArchived"
                  :title="isArchived ? '项目已归档，仅支持查看，不能删除任务' : '删除任务'"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 6H5H21M8 6V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V6M19 6V20C19 20.5304 18.7893 21.0391 18.4142 21.4142C18.0391 21.7893 17.5304 22 17 22H7C6.46957 22 5.96086 21.7893 5.58579 21.4142C5.21071 21.0391 5 20.5304 5 20V6H19Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
            </div>
            <div class="task-content" @click="openTaskDetailModal(task)">
              <h3 class="task-title">{{ task.title }}</h3>
              <p class="task-description">{{ task.description }}</p>
              <div class="task-meta">
                <span class="task-date" v-if="task.date">截止日期：{{ task.date }}</span>
                <span class="task-creator">创建人: {{ task.created_by_name }}</span>
                <span v-if="task.assignee_name" class="task-assignee">
                  负责人: {{ task.assignee_name }}
                </span>
                <span v-if="task.participantCount" class="task-participant-count">
                  接取人数: {{ task.assignees ? task.assignees.length : 0 }}/{{ task.participantCount }}
                </span>
            </div>
            </div>
            <!-- 任务操作区域 - 支持多人接取 -->
            <div class="task-assign-section" @click.stop>
              <!-- 已完成状态 -->
              <span v-if="task.status === '完成' || task.status === 'DONE' || task.status_value === 'DONE'" class="assign-status-badge completed">已完成</span>
              
              <!-- 当前用户已接取 -->
              <template v-else-if="isCurrentUserAssignee(task)">
              <span class="assign-status-badge assigned-by-me">已接取</span>
                <!-- 归档项目不显示任何操作按钮 -->
                <template v-if="!isArchived">
                  <!-- 逾期显示已逾期标识 -->
                  <span v-if="isTaskOverdue(task)" class="overdue-badge" style="margin-left: 8px;">已逾期</span>
                  <!-- 未逾期显示提交按钮 -->
                  <button v-else @click="openTaskSubmissionModal(task)" class="upload-result-btn" :title="(task.hasSubmission || task.status === '待审核' || task.status_value === 'PENDING_REVIEW') ? '更改提交' : '提交任务'">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 11L12 14L22 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                {{ (task.hasSubmission || task.status === '待审核' || task.status_value === 'PENDING_REVIEW') ? '更改提交' : '提交任务' }}
              </button>
                </template>
              </template>
              
              <!-- 当前用户未接取，但可以接取（项目未归档时才允许） -->
              <button
                v-else-if="!isArchived && canClaimTask(task)"
                @click="assignTask(task)"
                class="assign-btn"
                :title="isArchived ? '项目已归档，仅支持查看，不能接取任务' : '接取任务'"
              >
                接取任务
              </button>
              
              <!-- 任务已满员 -->
              <span v-else-if="isTaskFull(task)" class="assign-status-badge task-full">已满员</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 团队成员 -->
      <div class="section-card">
        <div class="section-header">
          <h2 class="section-title">团队成员</h2>
          <div class="section-actions">
            <!-- 邀请成员按钮：对所有管理员（OWNER和ADMIN）显示 -->
            <button 
              v-if="canManageProject" 
              class="btn primary admin-action" 
              @click="inviteMember"
              :disabled="isArchived"
              :title="isArchived ? '项目已归档，仅支持查看，不能邀请成员' : '邀请成员加入项目'"
              style="display: inline-flex; align-items: center; gap: 6px;"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M16 21V19C16 17.9391 15.5786 16.9217 14.8284 16.1716C14.0783 15.4214 13.0609 15 12 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M20 8V14M23 11H17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              邀请成员
            </button>
            <!-- 申请加入按钮：仅在当前用户不是成员时显示 -->
            <button
              v-if="!isArchived && !isProjectLocked && !isCurrentUserProjectMember && getCurrentUserId()"
              class="btn secondary apply-join-btn"
              type="button"
              @click.prevent="applyJoinProject"
              title="申请加入该项目"
            >
              申请加入
            </button>
        </div>
        </div>
        <div class="team-grid">
          <div v-for="member in teamMembers" :key="member.id" class="member-card">
            <div class="member-avatar" @click="goToUserProfile(member)">
              <img v-if="member.avatar" :src="member.avatar" :alt="member.name" />
              <div v-else class="avatar-placeholder">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
            <div class="member-info">
              <h4 class="member-name">{{ member.name }}</h4>
              <div class="member-role-container">
                <span class="role-badge" :class="getRoleBadgeClass(member.roleCode || member.role)">{{ getRoleDisplayName(member.roleCode || member.role) }}</span>
                <!-- OWNER用户：为普通成员显示明显的"设为管理员"按钮 -->
                <button 
                  v-if="canOperateAsOwner && !isAdminRole(member) && !isOwnerMember(member) && !isCurrentUser(member)"
                  class="btn-set-admin"
                  @click.stop="setMemberRole(member, 'ADMIN')"
                  title="将成员设为项目管理员"
                  type="button"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  设为管理员
                </button>
                <!-- OWNER用户：为管理员显示"移除管理员"按钮 -->
                <button 
                  v-if="canOperateAsOwner && isAdminRole(member) && !isOwnerMember(member) && !isCurrentUser(member)"
                  class="btn-remove-admin"
                  @click.stop="setMemberRole(member, 'MEMBER')"
                  title="移除管理员身份"
                  type="button"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  移除管理员
                </button>
                <!-- 角色管理下拉菜单（仅管理员可见，且不能修改OWNER和自己） -->
                <!-- 条件：是管理员 且 不是OWNER成员 且 不是当前用户 -->
                <!-- 对于OWNER用户，如果已经有明显的按钮，就不显示下拉菜单；对于ADMIN用户，仍然显示下拉菜单 -->
                <!-- 注意：OWNER用户已经通过明显按钮管理角色，所以不需要下拉菜单 -->
                <div 
                  v-if="!isProjectOwner && canManageProject && !isOwnerMember(member) && !isCurrentUser(member)" 
                  class="member-role-dropdown" 
                  @click.stop
                >
                  <button 
                    class="role-manage-btn" 
                    @click.stop="toggleMemberRoleDropdown(member.id)" 
                    title="管理角色"
                    type="button"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M12 8C13.1 8 14 7.1 14 6C14 4.9 13.1 4 12 4C10.9 4 10 4.9 10 6C10 7.1 10.9 8 12 8ZM12 10C10.9 10 10 10.9 10 12C10 13.1 10.9 14 12 14C13.1 14 14 13.1 14 12C14 10.9 13.1 10 12 10ZM12 16C10.9 16 10 16.9 10 18C10 19.1 10.9 20 12 20C13.1 20 14 19.1 14 18C14 16.9 13.1 16 12 16Z" fill="currentColor"/>
                    </svg>
                  </button>
                  <div v-if="memberRoleDropdownOpen[member.id]" class="role-dropdown-menu">
                    <!-- 只有OWNER可以将成员设置为ADMIN -->
                    <!-- 但对所有管理员显示，如果不是OWNER会显示为禁用状态 -->
                    <div 
                      v-if="!isAdminRole(member) && !isOwnerMember(member)" 
                      class="dropdown-item make-admin" 
                      :class="{ 'disabled': !canOperateAsOwner }"
                      @click.stop="!canOperateAsOwner ? null : setMemberRole(member, 'ADMIN')"
                      :title="!canOperateAsOwner ? '只有项目拥有者可以设置管理员' : '将成员设为项目管理员'"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      设为管理员
                      <span v-if="!isProjectOwner" class="permission-hint">(仅拥有者)</span>
            </div>
                    <!-- 只有OWNER可以移除ADMIN身份 -->
                    <div 
                      v-if="isAdminRole(member) && canOperateAsOwner" 
                      class="dropdown-item remove-admin" 
                      @click.stop="setMemberRole(member, 'MEMBER')"
                    >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
                      移除管理员
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- 移除成员按钮：管理员可以移除，但不能移除OWNER，ADMIN不能移除其他ADMIN -->
            <button 
              v-if="canManageProject && canRemoveMember(member)" 
              class="remove-member-btn" 
              @click.stop="removeTeamMember(member.id)" 
              :title="isOwnerMember(member) ? '不能移除项目拥有者' : '移除成员'"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <!-- 邀请成员占位符 -->
          <div v-for="invite in inviteSlots" :key="invite.id" class="member-card invite-card">
            <div class="member-avatar">
              <div class="avatar-placeholder">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
            <div class="member-info">
              <h4 class="member-name">{{ invite.role }}</h4>
              <p class="member-role">待邀请</p>
            </div>
            <button v-if="canManageProject" class="remove-member-btn" @click="removeInviteSlot(invite.id)" title="取消邀请">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
      </div>
    </div>
    <!-- 新建任务模态框 -->
    <div v-if="taskModalOpen" class="modal-overlay" @mousedown.self="onModalOverlayMouseDown" @mouseup.self="onModalOverlayMouseUp(closeTaskModal, $event)">
      <div class="modal-content" @mousedown.stop="onModalContentMouseDown" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">新建任务</h3>
          <button class="modal-close" @click="closeTaskModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">任务标题<span class="required-asterisk">*</span></label>
            <input 
              v-model="newTask.title" 
              type="text" 
              class="form-input" 
              placeholder="请输入任务标题"
              maxlength="50"
            />
          </div>
          <div class="form-field">
            <label class="form-label">任务描述</label>
            <textarea 
              v-model="newTask.description" 
              class="form-textarea" 
              placeholder="请输入任务描述"
              rows="3"
              maxlength="200"
            ></textarea>
          </div>
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">截止日期</label>
              <input 
                v-model="newTask.dueDate" 
                type="date" 
                :min="today"
                class="form-input"
                @change="validateNewTaskDueDate"
              />
              <div v-if="newTask.dateError" class="error-message">{{ newTask.dateError }}</div>
            </div>
            <div class="form-field">
              <label class="form-label">优先级</label>
              <select v-model="newTask.priority" class="form-select">
                <option value="高">高</option>
                <option value="中">中</option>
                <option value="低">低</option>
              </select>
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">任务人数</label>
            <input 
              v-model.number="newTask.participantCount" 
              type="number" 
              class="form-input" 
              placeholder="请输入可参加的成员数量"
              min="1"
              step="1"
            />
          </div>
          <div class="form-field form-field-switch">
            <label class="form-label">里程碑任务</label>
            <label class="switch-toggle">
              <input type="checkbox" v-model="newTask.isMilestone" />
              <span class="switch-slider"></span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" @click="closeTaskModal" class="btn btn-secondary">取消</button>
          <button type="button" @click="saveNewTask" class="btn btn-primary" :disabled="!newTask.title.trim() || isCreatingTask">
            {{ isCreatingTask ? '创建中...' : '创建任务' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 接取任务确认弹窗（替代浏览器 confirm） -->
    <div v-if="claimTaskConfirmOpen" class="modal-overlay" @click.self="cancelClaimTask">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">确认接取任务</h3>
          <button class="modal-close" @click="cancelClaimTask">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>确认接取任务
            <strong v-if="taskToClaim && taskToClaim.title">“{{ taskToClaim.title }}”</strong>
            吗？接取后该任务将标记为由你执行。
          </p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="cancelClaimTask">取消</button>
          <button type="button" class="btn btn-primary" @click="confirmClaimTask">确认接取</button>
        </div>
      </div>
    </div>

    <!-- 取消邀请成员确认弹窗（替代浏览器 confirm） -->
    <div v-if="removeInviteConfirmOpen" class="modal-overlay" @click.self="cancelRemoveInviteSlot">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">取消邀请成员</h3>
          <button class="modal-close" @click="cancelRemoveInviteSlot">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>确定要取消该成员的邀请吗？</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="cancelRemoveInviteSlot">保留邀请</button>
          <button type="button" class="btn btn-primary" @click="confirmRemoveInviteSlot">取消邀请</button>
        </div>
      </div>
    </div>

    <!-- 移除项目成员确认弹窗（替代浏览器 confirm） -->
    <div v-if="removeMemberConfirmOpen" class="modal-overlay" @click.self="cancelRemoveMember">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">移除项目成员</h3>
          <button class="modal-close" @click="cancelRemoveMember">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>
            确定要移除
            <strong v-if="memberToRemove && memberToRemove.name">{{ memberToRemove.name }}</strong>
            <span v-else>该成员</span>
            吗？移除后该成员将不再属于此项目。
          </p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="cancelRemoveMember">取消</button>
          <button type="button" class="btn btn-primary" @click="confirmRemoveMember">确认移除</button>
        </div>
      </div>
    </div>

    <!-- 删除项目确认弹窗（替代浏览器 confirm） -->
    <div v-if="deleteProjectConfirmOpen" class="modal-overlay" @click.self="cancelDeleteProject">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">删除项目</h3>
          <button class="modal-close" @click="cancelDeleteProject">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>确定要删除此项目吗？此操作不可撤销，项目及其所有数据将被永久删除。</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="cancelDeleteProject">取消</button>
          <button type="button" class="btn btn-primary" @click="confirmDeleteProject">确认删除</button>
        </div>
      </div>
    </div>
    <!-- 删除任务确认弹窗（替代浏览器 confirm） -->
    <div v-if="deleteTaskConfirmOpen" class="modal-overlay" @click.self="cancelDeleteTask">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">删除任务</h3>
          <button class="modal-close" @click="cancelDeleteTask">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>确定要删除此任务吗？此操作不可撤销。</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="cancelDeleteTask">取消</button>
          <button type="button" class="btn btn-primary" @click="confirmDeleteTask">确定</button>
        </div>
      </div>
    </div>
    <!-- 错误提示弹窗（替代浏览器 alert） -->
    <div v-if="errorDialogOpen" class="modal-overlay" @click.self="closeErrorDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">提示</h3>
          <button class="modal-close" @click="closeErrorDialog">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>{{ errorMessage }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" @click="closeErrorDialog">确定</button>
        </div>
      </div>
    </div>
    <!-- 角色变更确认弹窗（替代浏览器 confirm） -->
    <div v-if="roleChangeConfirmOpen" class="modal-overlay" @click.self="cancelRoleChange">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">{{ roleChangeTitle }}</h3>
          <button class="modal-close" @click="cancelRoleChange">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>{{ roleChangeMessage }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="cancelRoleChange">取消</button>
          <button type="button" class="btn btn-primary" @click="confirmRoleChange">确定</button>
        </div>
      </div>
    </div>
    <!-- 编辑项目模态框 -->
    <div v-if="editProjectModalOpen" class="modal-overlay" @mousedown.self="onModalOverlayMouseDown" @mouseup.self="onModalOverlayMouseUp(closeEditProjectModal, $event)">
      <div class="modal-content" @mousedown.stop="onModalContentMouseDown" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">编辑项目</h3>
          <button class="modal-close" @click="closeEditProjectModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">项目名称</label>
            <input 
              v-model="editProjectData.name" 
              type="text" 
              class="form-input" 
              placeholder="请输入项目名称"
              maxlength="100"
            />
          </div>
          <div class="form-field">
            <label class="form-label">项目描述</label>
            <textarea 
              v-model="editProjectData.description" 
              class="form-textarea" 
              placeholder="请输入项目描述"
              rows="3"
              maxlength="500"
            ></textarea>
          </div>
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">开始日期</label>
              <input 
                v-model="editProjectData.startDate" 
                type="date" 
                class="form-input"
              />
            </div>
            <div class="form-field">
              <label class="form-label">结束日期</label>
              <input 
                v-model="editProjectData.endDate" 
                type="date" 
                class="form-input"
              />
            </div>
          </div>
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">可见性</label>
              <select v-model="editProjectData.visibility" class="form-select">
                <option value="PUBLIC">公开</option>
                <option value="PRIVATE">私有</option>
              </select>
            </div>
            <div class="form-field">
              <label class="form-label">项目状态</label>
              <select v-model="editProjectData.status" class="form-select">
                <option value="PLANNING">规划中</option>
                <option value="ONGOING">进行中</option>
                <option value="COMPLETED">已完成</option>
                <option value="ARCHIVED">已归档</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" @click="closeEditProjectModal" class="btn btn-secondary">取消</button>
          <button type="button" @click="saveProjectUpdate" class="btn btn-primary" :disabled="!editProjectData.name.trim()">
            保存更改
          </button>
        </div>
      </div>
    </div>
    <!-- 编辑任务模态框 -->
    <div v-if="editTaskModalOpen" class="modal-overlay" @mousedown.self="onModalOverlayMouseDown" @mouseup.self="onModalOverlayMouseUp(closeEditTaskModal, $event)">
      <div class="modal-content task-modal" @mousedown.stop="onModalContentMouseDown" @click.stop>
        <div class="modal-header">
          <h3>编辑任务</h3>
          <button class="modal-close" @click="closeEditTaskModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">任务标题</label>
            <input
              type="text"
              v-model="editTaskData.title" 
              class="form-input"
              placeholder="请输入任务标题"
            />
          </div>
          <div class="form-field">
            <label class="form-label">任务描述</label>
            <textarea
              v-model="editTaskData.description"
              class="form-textarea"
              placeholder="请输入任务描述"
              rows="3"
            ></textarea>
          </div>
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">截止日期</label>
              <input
                type="date"
                v-model="editTaskData.dueDate"
                :min="today"
                class="form-input"
                @change="validateEditTaskDueDate"
              />
              <div v-if="editTaskData.dateError" class="error-message">{{ editTaskData.dateError }}</div>
            </div>
            <div class="form-field">
              <label class="form-label">优先级</label>
              <select v-model="editTaskData.priority" class="form-select">
                <option value="高">高</option>
                <option value="中">中</option>
                <option value="低">低</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" @click="closeEditTaskModal" class="btn btn-secondary">取消</button>
          <button type="button" @click="saveEditTask" class="btn btn-primary" :disabled="!editTaskData.title.trim()">
            保存更改
          </button>
        </div>
      </div>
    </div>
    <!-- 邀请成员弹窗 -->
    <div v-if="inviteMemberModalOpen" class="modal-overlay" @click.self="closeInviteMemberModal">
      <div class="modal-content invite-member-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">邀请成员</h3>
          <button class="modal-close" @click="closeInviteMemberModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 搜索用户 -->
          <div class="search-section">
            <label class="form-label">搜索用户</label>
            <div class="search-input-wrapper">
              <input 
                v-model="userSearchKeyword" 
                type="text" 
                class="form-input search-input" 
                placeholder="请输入用户ID或姓名进行搜索"
                @input="handleSearchInput"
              />
              <button class="search-btn" @click="searchUsers" :disabled="isSearching">
                <svg v-if="!isSearching" width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M21 21L16.65 16.65" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span v-else class="loading-spinner-small"></span>
              </button>
            </div>
          </div>
          <!-- 搜索结果列表 -->
          <div class="search-results" v-if="searchedUsers.length > 0">
            <div class="search-results-header">
              <span class="results-count">找到 {{ searchedUsers.length }} 个用户</span>
              <span class="selected-count" v-if="selectedUserIds.length > 0">已选 {{ selectedUserIds.length }} 人</span>
            </div>
            <div class="user-list">
              <div 
                v-for="user in displayedUsers" 
                :key="user.id || user.userId" 
                class="user-item"
                :class="{ 'user-selected': selectedUserIds.includes(user.id || user.userId) }"
                @click="selectUser(user)"
              >
                <div class="user-avatar">
                  <img v-if="user.avatarUrl || user.avatar || user.avatarData" :src="user.avatarUrl || user.avatar || user.avatarData" alt="用户头像" />
                  <div v-else class="avatar-placeholder">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                </div>
                <div class="user-info">
                  <div class="user-name">{{ user.name || user.username }}</div>
                  <div class="user-email">
                    <span v-if="user.email">{{ maskEmail(user.email) }}</span>
                    <span v-else>ID: {{ user.id || user.userId }}</span>
                  </div>
                </div>
                <div class="user-select-indicator" v-if="selectedUserIds.includes(user.id || user.userId)">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20 6L9 17L4 12" stroke="#4CAF50" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>
            <!-- 更多按钮 -->
            <div v-if="showMoreButton" class="load-more-container">
              <button class="btn load-more-btn" @click="loadMoreUsers">
                更多
              </button>
            </div>
          </div>
          <!-- 空状态 -->
          <div class="empty-state" v-else-if="hasSearched && !isSearching">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="11" cy="11" r="8" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M21 21L16.65 16.65" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p>未找到相关用户</p>
          </div>
          <!-- 提示信息 -->
          <div class="search-hint" v-else>
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="11" cy="11" r="8" stroke="#bbb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M21 21L16.65 16.65" stroke="#bbb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p>请输入用户ID或姓名进行搜索</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn secondary" @click="closeInviteMemberModal">取消</button>
          <button 
            class="btn primary" 
            @click="confirmInvite" 
            :disabled="selectedUserIds.length === 0 || isInviting"
          >
            <span v-if="!isInviting">确定邀请{{ selectedUserIds.length > 0 ? ' (' + selectedUserIds.length + ')' : '' }}</span>
            <span v-else>邀请中...</span>
          </button>
        </div>
      </div>
    </div>
    <!-- 任务详情弹窗 -->
    <div v-if="taskDetailModalOpen && selectedTask" class="modal-overlay" @click.self="closeTaskDetailModal">
      <div class="modal-content task-detail-modal" @click.stop>
        <div class="modal-header">
          <div class="task-detail-header-content">
            <h3 class="modal-title">任务详情</h3>
            <div class="task-detail-badges">
              <span class="task-priority-badge" :class="priorityClass(selectedTask.priority)">
                {{ selectedTask.priority }}
              </span>
              <span class="task-status-badge" :class="statusClass(selectedTask.status)">
                {{ selectedTask.status }}
              </span>
            </div>
          </div>
          <button class="modal-close" @click="closeTaskDetailModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body task-detail-body">
          <!-- 任务标题 -->
          <div class="task-detail-section task-title-section">
            <div class="task-title-row">
              <div class="task-title-value">{{ selectedTask.title }}</div>
              <span v-if="selectedTask.assignee_name" class="task-assignee-badge">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M16 21V19C16 17.9391 15.5786 16.9217 14.8284 16.1716C14.0783 15.4214 13.0609 15 12 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M20 8V14M23 11H17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                负责人: {{ selectedTask.assignee_name }}
              </span>
            </div>
            <div v-if="isTaskOverdue(selectedTask)" class="deadline-warning overdue">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M12 8V12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M12 16H12.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>已逾期</span>
            </div>
            <div v-else-if="isTaskNearDeadline(selectedTask)" class="deadline-warning near">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 8C18 6.4087 17.3679 4.88258 16.2426 3.75736C15.1174 2.63214 13.5913 2 12 2C10.4087 2 8.88258 2.63214 7.75736 3.75736C6.63214 4.88258 6 6.4087 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M13.73 21C13.5542 21.3031 13.3019 21.5547 12.9982 21.7295C12.6946 21.9044 12.3504 21.9965 12 21.9965C11.6496 21.9965 11.3054 21.9044 11.0018 21.7295C10.6982 21.5547 10.4458 21.3031 10.27 21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>即将到期</span>
            </div>
          </div>
          <!-- 任务描述 -->
          <div class="task-detail-section">
            <label class="task-detail-label">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M14 2V8H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M16 13H8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M16 17H8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M10 9H9H8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              任务描述
            </label>
            <div class="task-detail-value task-description-scroll">{{ selectedTask.description || '暂无描述' }}</div>
          </div>
          <!-- 信息卡片组 -->
          <div class="task-info-grid">
            <!-- 优先级 -->
            <div class="task-info-card">
              <div class="task-info-icon priority">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">优先级</div>
                <div class="task-info-value">
                  <span class="task-priority-badge" :class="priorityClass(selectedTask.priority)">
                    {{ selectedTask.priority }}
                  </span>
                </div>
              </div>
            </div>
            <!-- 状态 -->
            <div class="task-info-card">
              <div class="task-info-icon status">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">状态</div>
                <div class="task-info-value">
                  <span class="task-status-badge" :class="statusClass(selectedTask.status)">
                    {{ selectedTask.status }}
                  </span>
                </div>
              </div>
            </div>
            <!-- 截止日期 -->
            <div class="task-info-card" v-if="selectedTask.date || selectedTask.dueDate || selectedTask.due_date">
              <div class="task-info-icon deadline">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M19 4H5C3.89543 4 3 4.89543 3 6V20C3 21.1046 3.89543 22 5 22H19C20.1046 22 21 21.1046 21 20V6C21 4.89543 20.1046 4 19 4Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M16 2V6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M8 2V6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M3 10H21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M8 14H8.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">截止日期</div>
                <div class="task-info-value">
                  截止日期：{{ selectedTask.date || selectedTask.dueDate || selectedTask.due_date }}
                  <span v-if="isTaskOverdue(selectedTask)" class="overdue-badge">已逾期</span>
                </div>
              </div>
            </div>
            <!-- 提交工时 -->
            <div class="task-info-card">
              <div class="task-info-icon worktime">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M12 7V12L15 14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">提交工时</div>
                <div class="task-info-value">
                  <template v-if="selectedTaskWorktimeLoading">
                    加载中...
                  </template>
                  <template v-else-if="selectedTaskWorktimeInfo">
                    {{ selectedTaskWorktimeInfo.value }} 小时
                    <span class="worktime-meta" v-if="selectedTaskWorktimeInfo.submitter">（提交人：{{ selectedTaskWorktimeInfo.submitter }}）</span>
                  </template>
                  <template v-else>
                    暂无工时数据
                  </template>
                </div>
                <div class="task-info-extra" v-if="selectedTaskWorktimeInfo && selectedTaskWorktimeInfo.submittedAt">
                  提交时间：{{ selectedTaskWorktimeInfo.submittedAt }}
                </div>
              </div>
            </div>
            <!-- 创建人 -->
            <div class="task-info-card" v-if="selectedTask.created_by_name">
              <div class="task-info-icon creator">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">创建人</div>
                <div class="task-info-value">{{ selectedTask.created_by_name }}</div>
              </div>
            </div>
            <!-- 接取人数 -->
            <div class="task-info-card" v-if="selectedTask.participantCount">
              <div class="task-info-icon participants">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M17 21V19C17 17.9391 16.5786 16.9217 15.8284 16.1716C15.0783 15.4214 14.0609 15 13 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M23 21V19C22.9993 18.1137 22.7044 17.2528 22.1614 16.5523C21.6184 15.8519 20.8581 15.3516 20 15.13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M16 3.13C16.8604 3.35031 17.623 3.85071 18.1676 4.55232C18.7122 5.25392 19.0078 6.11683 19.0078 7.005C19.0078 7.89318 18.7122 8.75608 18.1676 9.45769C17.623 10.1593 16.8604 10.6597 16 10.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">接取人数</div>
                <div class="task-info-value">
                  <span :class="{ 'text-success': !isTaskFull(selectedTask), 'text-warning': isTaskFull(selectedTask) }">
                    {{ selectedTask.assignees ? selectedTask.assignees.length : 0 }}/{{ selectedTask.participantCount }}
                  </span>
                  <span v-if="isTaskFull(selectedTask)" class="task-full-badge">已满员</span>
                </div>
              </div>
            </div>
            <!-- 里程碑任务 -->
            <div class="task-info-card milestone-card">
              <div class="task-info-icon milestone">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <line x1="4" y1="22" x2="4" y2="15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content milestone-content">
                <div class="task-info-label">里程碑任务</div>
                <div class="task-info-value milestone-switch-wrapper">
                  <label class="switch-toggle" v-if="canManageProject">
                    <input type="checkbox" :checked="selectedTask.isMilestone" @change="toggleMilestone(selectedTask)" />
                    <span class="switch-slider"></span>
                  </label>
                  <span v-else class="milestone-status-text">{{ selectedTask.isMilestone ? '是' : '否' }}</span>
                </div>
              </div>
            </div>
            <!-- 统计信息 - 可点击查看详情 -->
            <div class="task-info-card clickable" @click="openTaskStatisticsModal(selectedTask)">
              <div class="task-info-icon statistics">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M18 20V10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M12 20V4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M6 20V14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="task-info-content">
                <div class="task-info-label">
                  统计信息
                  <span class="click-hint">点击查看详情</span>
                </div>
                <div class="task-info-value task-statistics-grid">
                  <div class="stat-item">
                    <span class="stat-label">执行者:</span>
                    <span class="stat-value">{{ selectedTask.assignees ? selectedTask.assignees.length : 0 }}人</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">提交状态:</span>
                    <span class="stat-value">{{ selectedTask.hasSubmission ? '已提交' : '未提交' }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">审核状态:</span>
                    <span class="stat-value">{{ getApprovalStatus(selectedTask) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <!-- 接取任务按钮（仅项目未归档时可见） -->
          <button
            v-if="!isArchived && canClaimTask(selectedTask)"
            @click="assignTask(selectedTask)"
            class="btn btn-success"
            style="margin-right: 12px;"
            :title="isArchived ? '项目已归档，仅支持查看，不能接取任务' : '接取任务'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right: 6px;">
              <path d="M16 21V19C16 17.9391 15.5786 16.9217 14.8284 16.1716C14.0783 15.4214 13.0609 15 12 15H5C3.93913 15 5.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M20 8V14M23 11H17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            接取任务
          </button>
          <!-- 更改提交按钮 - 已接取且未逾期时显示（归档项目不显示） -->
          <button 
            v-if="!isArchived && isCurrentUserAssignee(selectedTask) && !isTaskOverdue(selectedTask)" 
            @click="openTaskSubmissionModal(selectedTask)" 
            class="btn btn-success" 
            style="margin-right: 12px;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right: 6px;">
              <path d="M9 11L12 14L22 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            {{ (selectedTask.hasSubmission || selectedTask.status === '待审核' || selectedTask.status_value === 'PENDING_REVIEW') ? '更改提交' : '提交任务' }}
          </button>
          <button @click="closeTaskDetailModal" class="btn btn-primary">关闭</button>
        </div>
      </div>
    </div>
    </div>
    <!-- 任务统计详情弹窗 -->
    <div v-if="statisticsModalOpen && taskForStatistics" class="modal-overlay" @click.self="closeStatisticsModal">
      <div class="modal-content statistics-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">任务统计详情</h3>
          <button class="modal-close" @click="closeStatisticsModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body statistics-modal-body">
          <!-- 多人任务分配标题 -->
          <div class="statistics-section">
            <h4 class="statistics-task-title">多人任务分配</h4>
          </div>
          
          <!-- 统计概览 -->
          <div class="statistics-section" v-if="taskStats">
            <div class="section-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 20V10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M12 20V4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6 20V14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <h5 class="section-title">统计概览</h5>
            </div>
            <div class="stats-overview">
              <div class="stats-grid">
                <div class="stat-card">
                  <div class="stat-card-label">执行者总数</div>
                  <div class="stat-card-value">{{ taskStats.totalExecutors || 0 }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-card-label">最终提交数</div>
                  <div class="stat-card-value">{{ taskStats.totalFinalSubmissions || 0 }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-card-label">已批准提交</div>
                  <div class="stat-card-value approved">{{ taskStats.approvedFinalSubmissions || 0 }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 所有执行者 -->
          <div class="statistics-section">
            <div class="section-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M17 21V19C17 17.9391 16.5786 16.9217 15.8284 16.1716C15.0783 15.4214 14.0609 15 13 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M23 21V19C22.9993 18.1137 22.7044 17.2528 22.1614 16.5523C21.6184 15.8519 20.8581 15.3516 20 15.13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M16 3.13C16.8604 3.35031 17.623 3.85071 18.1676 4.55232C18.7122 5.25392 19.0078 6.11683 19.0078 7.005C19.0078 7.89318 18.7122 8.75608 18.1676 9.45769C17.623 10.1593 16.8604 10.6597 16 10.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <h5 class="section-title">所有执行者 ({{ taskStats ? taskStats.totalExecutors : (taskForStatistics.assignees ? taskForStatistics.assignees.length : 0) }}人)</h5>
            </div>
            <div class="assignees-list">
              <div v-if="taskForStatistics.assignees && taskForStatistics.assignees.length > 0" class="assignee-items">
                <div v-for="(assignee, index) in taskForStatistics.assignees" :key="index" class="assignee-item">
                  <!-- 显示头像：优先使用后端返回的头像URL，否则显示首字母 -->
                  <div v-if="assignee.avatarUrl" class="assignee-avatar-img">
                    <img :src="assignee.avatarUrl" :alt="assignee.userName" class="avatar-image" />
                  </div>
                  <div v-else class="assignee-avatar">{{ assignee.userName ? assignee.userName.charAt(0) : '?' }}</div>
                  <div class="assignee-info">
                    <div class="assignee-name">{{ assignee.userName }}</div>
                    <div class="assignee-meta">
                      <span class="assignee-type">{{ assignee.assignType === 'CLAIMED' ? '主动接取' : '管理员分配' }}</span>
                      <span class="assignee-time">{{ formatTime(assignee.assignedAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="empty-state">暂无执行者</div>
            </div>
          </div>

          <!-- 最终提交信息 -->
          <div class="statistics-section">
            <div class="section-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 11L12 14L22 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <h5 class="section-title">最终提交信息</h5>
            </div>
            <div v-if="approvedSubmission" class="submission-details">
              <!-- 提交者信息 -->
              <div class="submission-meta">
                <div class="meta-item">
                  <span class="meta-label">提交人:</span>
                  <span class="meta-value">{{ approvedSubmission.submitterName }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">提交时间:</span>
                  <span class="meta-value">{{ formatTime(approvedSubmission.submittedAt) }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">审核状态:</span>
                  <span class="meta-value status-approved">已批准</span>
                </div>
              </div>
              
              <!-- 提交内容 -->
              <div class="submission-content">
                <div class="content-label">提交内容:</div>
                <div class="content-text">{{ approvedSubmission.content || '无' }}</div>
              </div>
              
              <!-- 附件列表 -->
              <div v-if="approvedSubmission.attachments && approvedSubmission.attachments.length > 0" class="submission-attachments">
                <div class="content-label">附件 ({{ approvedSubmission.attachments.length }}):</div>
                <div class="attachment-list">
                  <div v-for="(attachmentUrl, index) in approvedSubmission.attachments" :key="index" class="attachment-item">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M13 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V9L13 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M13 2V9H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <a :href="attachmentUrl" target="_blank" class="attachment-name">{{ getFileNameFromUrl(attachmentUrl) }}</a>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              {{ taskForStatistics.hasSubmission ? '暂无已批准的提交' : '暂无提交' }}
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeStatisticsModal" class="btn btn-primary">关闭</button>
      </div>
    </div>
    </div>
    <!-- 分配任务模态框 -->
    <div v-if="assignTaskModalOpen && taskToAssign" class="modal-overlay" @click.self="closeAssignTaskModal">
      <div class="modal-content assign-task-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">分配任务</h3>
          <button class="modal-close" @click="closeAssignTaskModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="assign-task-info">
            <h4 class="assign-task-title">{{ taskToAssign.title }}</h4>
            <p class="assign-task-description">{{ taskToAssign.description || '暂无描述' }}</p>
            <div class="task-assignee-status">
              <span class="status-label">当前执行者:</span>
              <span class="status-value">{{ taskToAssign.assignee_name || '暂无' }}</span>
              <span v-if="taskToAssign.participantCount" class="status-count">
                ({{ taskToAssign.assignees ? taskToAssign.assignees.length : 0 }}/{{ taskToAssign.participantCount }})
              </span>
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">添加执行者</label>
            <div class="member-select-list">
              <div 
                v-for="member in teamMembers" 
                :key="member.id" 
                class="member-select-item"
                :class="{ 
                  selected: selectedAssigneeId === member.id,
                  'already-assigned': taskToAssign.assignee_id && taskToAssign.assignee_id.includes(String(member.id))
                }"
                @click="selectedAssigneeId = member.id"
              >
                <div class="member-select-avatar">
                  <img v-if="member.avatar" :src="member.avatar" :alt="member.name" />
                  <div v-else class="avatar-placeholder">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                </div>
                <div class="member-select-info">
                  <div class="member-select-name">
                    {{ member.name }}
                    <span v-if="taskToAssign.assignee_id && taskToAssign.assignee_id.includes(String(member.id))" class="already-assigned-badge">已分配</span>
                  </div>
                  <div class="member-select-role">{{ member.role }}</div>
                </div>
                <div class="member-select-indicator" v-if="selectedAssigneeId === member.id">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20 6L9 17L4 12" stroke="#4CAF50" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>
            <div v-if="teamMembers.length === 0" class="empty-members-hint">
              <p>暂无团队成员，请先邀请成员加入项目</p>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeAssignTaskModal" class="btn secondary">取消</button>
          <button @click="confirmAssignTask" class="btn primary" :disabled="!selectedAssigneeId">确认分配</button>
        </div>
      </div>
    </div>
    <!-- 任务提交弹窗 (新) -->
    <TaskSubmissionModal
      :visible.sync="taskSubmissionModalVisible"
      :task="taskToSubmit || {}"
      :latest-submission="latestSubmissionForEdit || null"
      @close="closeTaskSubmissionModal"
      @success="handleTaskSubmitSuccess"
    />
    <!-- 任务审核弹窗 (新) -->
    <TaskSubmissionReviewModal
      :visible.sync="taskReviewModalVisible"
      :submission="submissionToReview || {}"
      @close="closeTaskReviewModal"
      @success="handleReviewSuccess"
    />
    <!-- 成功提示Toast -->
    <div v-if="showToast" class="success-toast">
      {{ toastMessage }}
      </div>
    <!-- 图片裁切Modal -->
    <div v-if="showCropModal" class="crop-modal-overlay">
      <div class="crop-modal-content" @click.stop>
        <div class="crop-modal-header">
          <h3>裁切项目图片</h3>
          <p class="crop-hint">请拖拽选择裁切区域，确保比例与项目广场显示一致</p>
        </div>
        <div class="crop-modal-body">
          <div class="crop-container">
            <canvas ref="cropCanvas" class="crop-canvas"></canvas>
            <div class="crop-overlay" ref="cropOverlay">
              <div class="crop-selection" ref="cropSelection">
                <!-- 调整大小的控制点 -->
                <div class="resize-handle resize-handle-nw" data-handle="nw"></div>
                <div class="resize-handle resize-handle-ne" data-handle="ne"></div>
                <div class="resize-handle resize-handle-sw" data-handle="sw"></div>
                <div class="resize-handle resize-handle-se" data-handle="se"></div>
              </div>
            </div>
          </div>
        </div>
        <div class="crop-modal-footer">
          <button class="btn-cancel" @click="closeCropModal">重新选择图片</button>
          <button class="btn-confirm" @click="applyCrop">完成裁切</button>
      </div>
    </div>
    </div>
  </div>
</template>
<script>
import '@/assets/styles/ProjectDetail.css'
import { normalizeProjectCoverUrl, normalizeImageUrl, getDefaultProjectImage, preloadImages } from '@/utils/imageUtils'
import { addTimestampToUrl } from '@/utils/imageUtils'
import { cacheProjectCoverIfNeeded, getCachedProjectCover, saveProjectCover } from '@/utils/projectImageCache'
import TaskSubmissionModal from '@/components/TaskSubmissionModal.vue'
import TaskSubmissionReviewModal from '@/components/TaskSubmissionReviewModal.vue'
import { getTaskSubmissions, getLatestSubmission } from '@/api/taskSubmission'
export default {
  name: 'ProjectDetail',
  components: {
    TaskSubmissionModal,
    TaskSubmissionReviewModal
  },
  data() {
    return {
      // 禁用所有动画和特效
      enableBackgroundEffects: false,
      disableAllAnimations: true,
      // 任务加载状态
      isLoadingTasks: false,
      operationLoadingCount: 0,
      operationLoadingText: '',
      modalOverlayMouseDownOnSelf: false,
      taskTypeOpen: false,
      selectedTaskType: '',
      statusDropdownOpen: false,
      taskModalOpen: false,
      editProjectModalOpen: false,
      showToast: false,
      toastMessage: '',
      newTask: {
        title: '',
        description: '',
        dueDate: '',
        priority: '中',
        status: '待接取',
        dateError: '',
        isMilestone: false
      },
      editProjectData: {
        name: '',
        description: '',
        startDate: '',
        endDate: '',
        visibility: 'PUBLIC',
        status: 'ONGOING'
      },
      project: null,
      projectStatus: null,
      isArchived: false,
      tasks: [],
      teamMembers: [],
      inviteSlots: [],
      isLoading: true,
      taskListModalOpen: false,
      isCreatingTask: false, // 防止重复点击创建任务
      taskDetailModalOpen: false, // 任务详情弹窗
      selectedTask: null, // 当前选中的任务
      selectedTaskWorktimeInfo: null, // 当前任务的最新工时信息
      selectedTaskWorktimeLoading: false, // 工时信息加载状态
      statisticsModalOpen: false, // 统计详情弹窗
      taskForStatistics: null, // 统计详情的任务
      approvedSubmission: null, // 已批准的提交信息
      taskStats: null, // 任务统计信息（来自后端）
      editTaskModalOpen: false, // 编辑任务弹窗
      editTaskData: {
        title: '',
        description: '',
        dueDate: '',
        priority: '中',
        taskId: null
      },
      // 邀请成员相关
      inviteMemberModalOpen: false, // 邀请成员弹窗
      userSearchKeyword: '', // 用户搜索关键词
      searchedUsers: [], // 搜索到的用户列表
      selectedUserIds: [], // 选中的用户ID数组（支持多选）
      isSearching: false, // 搜索中状态
      isInviting: false, // 邀请中状态
      hasSearched: false, // 是否已经搜索过
      searchDebounceTimer: null, // 搜索防抖定时器
      displayedUserCount: 4, // 当前显示的用户数量
      // 图片裁切相关
      showCropModal: false, // 是否显示裁切模态框
      originalImage: null, // 原始图片对象
      originalImageData: null, // 原始图片数据（DataURL）
      cropData: {
        x: 0,
        y: 0,
        width: 0,
        height: 0
      },
      // 分配任务相关
      assignTaskModalOpen: false, // 分配任务模态框
      taskToAssign: null, // 待分配的任务
      selectedAssigneeId: null, // 选中的负责人ID
      // 任务提交相关 (新)
      taskSubmissionModalVisible: false,
      taskToSubmit: null,
      taskSubmissions: [],
      // 任务审核相关 (新)
      taskReviewModalVisible: false,
      submissionToReview: null,
      // 用于编辑提交的最新提交数据
      latestSubmissionForEdit: null,
      // 接取任务确认弹窗
      claimTaskConfirmOpen: false,
      taskToClaim: null,
      // 取消邀请成员确认弹窗
      removeInviteConfirmOpen: false,
      inviteSlotToRemove: null,
      // 移除项目成员确认弹窗
      removeMemberConfirmOpen: false,
      memberToRemove: null,
      // 删除项目确认弹窗
      deleteProjectConfirmOpen: false,
      // 删除任务确认弹窗
      deleteTaskConfirmOpen: false,
      taskToDelete: null,
      // 错误提示弹窗
      errorDialogOpen: false,
      errorMessage: '',
      // 角色变更确认弹窗
      roleChangeConfirmOpen: false,
      roleChangeTitle: '',
      roleChangeMessage: '',
      memberToChangeRole: null,
      newRoleToSet: null,
      // 权限相关
      isAdmin: false, // 当前用户是否为项目管理员（包括OWNER和ADMIN）
      isOwner: false, // 当前用户是否为项目拥有者
      // 角色管理相关
      memberRoleDropdownOpen: {} // 记录每个成员的角色下拉菜单是否打开 { memberId: boolean }
    }
  },
  computed: {
    isOperationLoading() {
      return this.operationLoadingCount > 0
    },
    filteredTasks() {
      let tasks = this.tasks
      if (this.selectedTaskType) {
        tasks = tasks.filter(task => task.priority === this.selectedTaskType)
      }
      // 按创建时间排序，返回所有任务（用于横向滚动展示全部任务）
      return tasks
        .sort((a, b) => new Date(b.created_at || b.id) - new Date(a.created_at || a.id))
    },
    allTasks() {
      // 已不再用于“更多”按钮，仅保留兼容性
      return this.tasks
    },
    taskCount() {
      return Array.isArray(this.tasks) ? this.tasks.length : 0
    },
    participantCount() {
      if (Array.isArray(this.teamMembers) && this.teamMembers.length > 0) {
        return this.teamMembers.length
      }
    },
    isProjectLocked() {
      const status = String(this.projectStatus || this.project?.status || '').toUpperCase()
      const completedStatus = status === 'COMPLETED' || status === 'DONE' || status === '已完成'
      return this.isArchived === true || completedStatus
    },
    canManageProject() {
      // 只检查是否已归档，不检查是否已完成，允许已完成的项目仍然可以编辑
      return !this.isArchived && this.isProjectManager
    },
    canOperateAsOwner() {
      // 只检查是否已归档，不检查是否已完成，允许已完成的项目仍然可以删除
      return !this.isArchived && this.isProjectOwner
    },
    // 当前用户是否已经是项目成员（用于控制“申请加入”按钮显隐）
    isCurrentUserProjectMember() {
      const currentUserId = this.getCurrentUserId()
      if (!currentUserId || !Array.isArray(this.teamMembers)) {
        return false
      }
      const userIdStr = String(currentUserId)
      return this.teamMembers.some(member => {
        const memberId = String(member.id || member.userId || '')
        return memberId === userIdStr
      })
    },
    // 显示的用户列表（分页）
    displayedUsers() {
      if (!Array.isArray(this.searchedUsers)) {
        return []
      }
      return this.searchedUsers.slice(0, this.displayedUserCount)
    },
    // 是否显示"更多"按钮
    showMoreButton() {
      return Array.isArray(this.searchedUsers) && this.searchedUsers.length > this.displayedUserCount
    },
    isProjectManager() {
      // 判断当前用户是否是项目管理员（包括OWNER和ADMIN）
      // 方法1：使用已设置的 isAdmin 状态（最快）
      if (this.isAdmin === true || this.isOwner === true) {
        console.log('[isProjectManager] 从 isAdmin/isOwner 判断为管理员:', { isAdmin: this.isAdmin, isOwner: this.isOwner })
        return true
      }
      // 方法2：从团队成员列表中实时判断（最可靠）
      const currentUserId = this.getCurrentUserId()
      if (currentUserId && this.teamMembers && this.teamMembers.length > 0) {
        const currentMember = this.teamMembers.find(m => {
          const memberId = String(m.id || m.userId || '')
          const memberUserId = String(m.userId || '')
          const userId = String(currentUserId)
          return memberId === userId || memberUserId === userId
        })
        if (currentMember) {
          const roleCode = String(currentMember.roleCode || '').toUpperCase()
          const roleName = String(currentMember.role || '').toUpperCase()
          // 检查是否为OWNER或ADMIN
          const isOwnerRole = roleCode === 'OWNER' || 
                             roleName.includes('OWNER') || 
                             roleName.includes('拥有者') || 
                             roleName.includes('负责人') ||
                             currentMember.role === '项目拥有者' ||
                             currentMember.role === '项目负责人'
          const isAdminRole = roleCode === 'ADMIN' || 
                             roleName.includes('ADMIN') || 
                             roleName.includes('管理员') ||
                             currentMember.role === '项目管理员'
          if (isOwnerRole || isAdminRole) {
            // 注意：在计算属性中不应该直接修改数据属性
            // 这里只返回判断结果，实际的权限状态更新应该在 loadTeamMembers 或 checkAdminPermission 中进行
            console.log('[isProjectManager] 从成员列表判断为管理员:', { 
              roleCode, 
              roleName, 
              isOwnerRole, 
              isAdminRole,
              currentIsAdmin: this.isAdmin,
              currentIsOwner: this.isOwner
            })
            return true
          }
        }
      }
      // 方法3：使用旧的判断逻辑（兼容性）
      const currentUserName = this.getCurrentUserName()
      if (this.project && this.project.manager === currentUserName) {
        console.log('[isProjectManager] 从 project.manager 判断为管理员')
        return true
      }
      console.log('[isProjectManager] 不是管理员:', {
        isAdmin: this.isAdmin,
        isOwner: this.isOwner,
        teamMembersCount: this.teamMembers?.length || 0,
        currentUserId: currentUserId,
        projectManager: this.project?.manager,
        currentUserName: currentUserName
      })
      return false
    },
    // 判断当前用户是否为项目拥有者
    isProjectOwner() {
      // 优先从团队成员列表中判断
      const currentUserId = this.getCurrentUserId()
      if (currentUserId && this.teamMembers && this.teamMembers.length > 0) {
        const currentMember = this.teamMembers.find(m => {
          const memberId = String(m.id || m.userId || '')
          const userId = String(currentUserId)
          return memberId === userId
        })
        if (currentMember) {
          const role = String(currentMember.roleCode || currentMember.role || '').toUpperCase()
          const roleName = String(currentMember.role || '').toUpperCase()
          if (role === 'OWNER' || roleName.includes('OWNER') || roleName.includes('拥有者') || roleName.includes('负责人')) {
            return true
          }
        }
      }
      // 如果从团队成员列表无法判断，使用API返回的结果
      if (this.isOwner === true) {
        return true
      }
      return false
    },
    // 获取今天的日期，格式为 YYYY-MM-DD
    today() {
      const today = new Date()
      return today.toISOString().split('T')[0]
    }
  },
  async mounted() {
    // 立即加载任务列表，不等待成员权限
    const projectId = this.$route.params.id;
    if (projectId) {
      // 并行加载项目详情和任务列表
      Promise.all([
        this.loadProject(),
        this.loadProjectTasks()
      ]).catch(error => {
        console.error('加载项目数据时出错:', error);
      });
    } else {
      await this.loadProject();
    }
    
    document.addEventListener('click', this.handleClickOutside);
    // 监听精确的头像更新事件
    this.$eventBus.on(
      this.$EventTypes.USER_AVATAR_UPDATED, 
      this.handleAvatarUpdated,
      { debounce: 300 } // 300ms防抖，避免频繁更新
    );
    // 监听任务状态更新事件（从其他页面触发，如任务审核页面）
    this.$eventBus.on(
      this.$EventTypes.TASK_UPDATED,
      this.handleTaskStatusUpdated,
      { debounce: 500 } // 500ms防抖，避免频繁刷新
    );
  },
  beforeRouteUpdate(to, from, next) {
    // 当路由中的项目ID发生变化时，先清空当前详情，再加载新项目，避免短暂显示上一个项目
    if (to.params && to.params.id && to.params.id !== from.params.id) {
      this.isLoading = true
      this.project = null
      this.tasks = []
      this.teamMembers = []
      this.inviteSlots = []
      this.loadProject(to.params.id)
    }
    next()
  },
  beforeDestroy() {
    document.removeEventListener('click', this.handleClickOutside)
    // 取消事件监听
    this.$eventBus.off(this.$EventTypes.USER_AVATAR_UPDATED, this.handleAvatarUpdated)
  },
  methods: {
    onModalOverlayMouseDown() {
      this.modalOverlayMouseDownOnSelf = true
    },
    onModalContentMouseDown() {
      this.modalOverlayMouseDownOnSelf = false
    },
    onModalOverlayMouseUp(closeFn, event) {
      const shouldClose = this.modalOverlayMouseDownOnSelf && event && event.target === event.currentTarget
      this.modalOverlayMouseDownOnSelf = false
      if (shouldClose && typeof closeFn === 'function') {
        closeFn()
      }
    },
    beginOperationLoading(text) {
      this.operationLoadingCount += 1
      if (text) {
        this.operationLoadingText = text
      }
    },
    endOperationLoading() {
      this.operationLoadingCount = Math.max(0, this.operationLoadingCount - 1)
      if (this.operationLoadingCount === 0) {
        this.operationLoadingText = ''
      }
    },
    async withOperationLoading(text, fn) {
      this.beginOperationLoading(text)
      try {
        return await fn()
      } finally {
        this.endOperationLoading()
      }
    },
    openTaskListModal() {
      this.taskListModalOpen = true
    },
    closeTaskListModal() {
      this.taskListModalOpen = false
    },
    async openTaskDetailModal(task) {
      this.selectedTask = task
      this.taskDetailModalOpen = true
      this.selectedTaskWorktimeInfo = null
      const taskId = task && task.id
      if (!taskId) {
        this.selectedTaskWorktimeLoading = false
        return
      }
      
      // 获取任务的提交状态
      await this.updateTaskSubmissionStatus(taskId)
      
      // 获取工时信息
      await this.fetchTaskWorktime(taskId)
    },
    /**
     * 批量更新任务状态（仅更新待审核状态的任务，避免性能问题）
     */
    /**
     * 批量更新任务状态
     * @param {Array} taskIds - 需要更新状态的任务ID数组
     */
    async batchUpdateTaskStatus(taskIds = []) {
      if (!taskIds || taskIds.length === 0) {
        console.log('[batchUpdateTaskStatus] 没有需要更新的任务')
        return
      }

      console.log(`[batchUpdateTaskStatus] 开始批量更新 ${taskIds.length} 个任务的状态`)
      
      try {
        // 批量获取任务提交状态
        const { batchGetTaskSubmissions } = await import('@/api/taskSubmission')
        const response = await batchGetTaskSubmissions(taskIds)
        
        if (response?.code === 200 && response.data) {
          const submissionsMap = response.data
          let updatedCount = 0
          
          // 遍历所有任务，更新状态
          this.tasks.forEach((task, index) => {
            const taskSubmissions = submissionsMap[task.id] || []
            const hasApprovedSubmission = taskSubmissions.some(s => 
              s.reviewStatus === 'APPROVED' && s.isFinal === true
            )
            
            if (hasApprovedSubmission) {
              // 更新任务状态为"完成"
              this.$set(this.tasks[index], 'status', '完成')
              this.$set(this.tasks[index], 'status_value', 'DONE')
              this.$set(this.tasks[index], 'hasApprovedSubmission', true)
              updatedCount++
            }
          })
          
          console.log(`[batchUpdateTaskStatus] 成功更新了 ${updatedCount} 个任务的状态`)
        } else {
          console.warn('[batchUpdateTaskStatus] 批量获取任务提交状态失败，回退到单任务查询')
          // 如果批量获取失败，回退到单任务查询
          await this.fallbackToSingleTaskUpdate(taskIds)
        }
      } catch (error) {
        console.error('[batchUpdateTaskStatus] 批量更新失败:', error)
        // 发生错误时尝试回退到单任务查询
        await this.fallbackToSingleTaskUpdate(taskIds)
      }
    },
    
    /**
     * 回退到单任务状态更新
     * @param {Array} taskIds - 需要更新的任务ID数组
     */
    async fallbackToSingleTaskUpdate(taskIds) {
      console.log('[fallbackToSingleTaskUpdate] 开始单任务状态更新')
      const BATCH_SIZE = 3 // 限制并发数
      
      for (let i = 0; i < taskIds.length; i += BATCH_SIZE) {
        const batch = taskIds.slice(i, i + BATCH_SIZE)
        await Promise.all(batch.map(taskId => this.updateSingleTaskStatus(taskId)))
      }
    },
    
    /**
     * 更新单个任务的状态
     * @param {string} taskId - 任务ID
     */
    async updateSingleTaskStatus(taskId) {
      try {
        const { getTaskSubmissions } = await import('@/api/taskSubmission')
        const response = await getTaskSubmissions(taskId)
        
        if (response?.code === 200 && response.data) {
          const submissions = response.data
          const hasApprovedSubmission = submissions.some(s => 
            s.reviewStatus === 'APPROVED' && s.isFinal === true
          )
          
          if (hasApprovedSubmission) {
            const taskIndex = this.tasks.findIndex(t => t.id === taskId)
            if (taskIndex !== -1) {
              this.$set(this.tasks[taskIndex], 'status', '完成')
              this.$set(this.tasks[taskIndex], 'status_value', 'DONE')
              this.$set(this.tasks[taskIndex], 'hasApprovedSubmission', true)
            }
          }
        }
      } catch (error) {
        console.error(`[updateSingleTaskStatus] 更新任务 ${taskId} 状态失败:`, error)
      }
    },
    /**
     * 更新任务的提交状态信息
     */
    async updateTaskSubmissionStatus(taskId) {
      try {
        const { getTaskSubmissions } = await import('@/api/taskSubmission')
        const response = await getTaskSubmissions(taskId)
        
        if (response && response.code === 200 && response.data) {
          const submissions = response.data
          
          // 检查是否有提交记录
          const hasSubmission = submissions.length > 0
          
          // 检查是否有最终提交
          const hasFinalSubmission = submissions.some(s => s.isFinal === true)
          
          // 检查是否有已批准的最终提交
          const hasApprovedSubmission = submissions.some(s => 
            s.reviewStatus === 'APPROVED' && s.isFinal === true
          )
          
          // 更新 selectedTask 的提交状态
          if (this.selectedTask) {
            this.selectedTask.hasSubmission = hasSubmission
            this.selectedTask.hasFinalSubmission = hasFinalSubmission
            this.selectedTask.hasApprovedSubmission = hasApprovedSubmission
            
            // 如果有已批准的提交，更新任务状态为"完成"
            if (hasApprovedSubmission) {
              this.selectedTask.status = '完成'
              this.selectedTask.status_value = 'DONE'
            } else if (hasFinalSubmission) {
              // 如果有最终提交但未批准，状态应该是"待审核"
              this.selectedTask.status = '待审核'
              this.selectedTask.status_value = 'PENDING_REVIEW'
            }
            
            // 同步更新任务列表中的对应任务
            const taskIndex = this.tasks.findIndex(t => t.id === taskId)
            if (taskIndex !== -1) {
              this.$set(this.tasks[taskIndex], 'hasSubmission', hasSubmission)
              this.$set(this.tasks[taskIndex], 'hasFinalSubmission', hasFinalSubmission)
              this.$set(this.tasks[taskIndex], 'hasApprovedSubmission', hasApprovedSubmission)
              
              if (hasApprovedSubmission) {
                this.$set(this.tasks[taskIndex], 'status', '完成')
                this.$set(this.tasks[taskIndex], 'status_value', 'DONE')
              } else if (hasFinalSubmission) {
                this.$set(this.tasks[taskIndex], 'status', '待审核')
                this.$set(this.tasks[taskIndex], 'status_value', 'PENDING_REVIEW')
              }
            }
          }
          
          console.log('[updateTaskSubmissionStatus] 任务提交状态已更新:', {
            hasSubmission,
            hasFinalSubmission,
            hasApprovedSubmission,
            status: this.selectedTask?.status
          })
        }
      } catch (error) {
        console.error('更新任务提交状态失败:', error)
      }
    },
    closeTaskDetailModal() {
      this.taskDetailModalOpen = false
      this.selectedTask = null
      this.selectedTaskWorktimeInfo = null
      this.selectedTaskWorktimeLoading = false
    },
    /**
     * 切换任务的里程碑状态
     */
    async toggleMilestone(task) {
      const newValue = !task.isMilestone
      try {
        const { taskAPI } = await import('@/api/task')
        const response = await taskAPI.updateTask(task.id, {
          isMilestone: newValue
        })
        if (response && response.code === 200) {
          // 更新本地任务数据
          task.isMilestone = newValue
          // 同步更新任务列表中的数据
          const taskInList = this.tasks.find(t => t.id === task.id)
          if (taskInList) {
            taskInList.isMilestone = newValue
          }
          this.showSuccessToast(newValue ? '已设为里程碑任务' : '已取消里程碑任务')
        } else {
          this.showSuccessToast('更新失败：' + (response.msg || '未知错误'))
        }
      } catch (error) {
        console.error('切换里程碑状态失败:', error)
        this.showSuccessToast('操作失败，请稍后重试')
      }
    },
    /**
     * 打开任务统计详情弹窗
     */
    async openTaskStatisticsModal(task) {
      this.taskForStatistics = task
      this.statisticsModalOpen = true
      this.approvedSubmission = null
      this.taskStats = null
      
      // 获取任务详情（包含执行者的完整信息：姓名、头像等）
      await this.fetchTaskDetailForStats(task.id)
      
      // 获取任务统计信息（从后端）
      await this.fetchTaskStats(task.id)
      
      // 获取已批准的提交信息
      await this.fetchApprovedSubmission(task.id)
    },
    /**
     * 关闭统计详情弹窗
     */
    /**
     * 获取任务详情（用于统计弹窗，包含执行者完整信息）
     */
    async fetchTaskDetailForStats(taskId) {
      try {
        const { taskAPI } = await import('@/api/task')
        const response = await taskAPI.getTaskDetail(taskId)
        
        if (response && response.code === 200 && response.data) {
          // 更新 taskForStatistics，使用后端返回的最新数据（包含任务标题、执行者的姓名、头像等）
          this.taskForStatistics = {
            ...this.taskForStatistics,
            title: response.data.title || this.taskForStatistics.title,  // 使用后端返回的任务标题
            assignees: response.data.assignees || []  // 使用后端返回的最新执行者信息
          }
          console.log('[fetchTaskDetailForStats] 获取到任务详情:', {
            title: this.taskForStatistics.title,
            assignees: this.taskForStatistics.assignees
          })
        }
      } catch (error) {
        console.error('获取任务详情失败:', error)
      }
    },
    closeStatisticsModal() {
      this.statisticsModalOpen = false
      this.taskForStatistics = null
      this.approvedSubmission = null
      this.taskStats = null
    },
    /**
     * 获取任务统计信息
     */
    async fetchTaskStats(taskId) {
      try {
        const { getTaskSubmissionStats } = await import('@/api/taskSubmission')
        const response = await getTaskSubmissionStats(taskId)
        
        if (response && response.code === 200 && response.data) {
          this.taskStats = response.data
          console.log('[fetchTaskStats] 统计信息:', this.taskStats)
        }
      } catch (error) {
        console.error('获取任务统计信息失败:', error)
      }
    },
    /**
     * 获取已批准的提交信息
     */
    async fetchApprovedSubmission(taskId) {
      try {
        const { getTaskSubmissions } = await import('@/api/taskSubmission')
        const response = await getTaskSubmissions(taskId)
        
        if (response && response.code === 200 && response.data) {
          const submissions = response.data
          // 筛选出已批准且是最终提交的记录
          const approvedFinalSubmissions = submissions.filter(s => 
            s.reviewStatus === 'APPROVED' && s.isFinal === true
          )
          
          if (approvedFinalSubmissions.length > 0) {
            // 取最新的一条已批准提交（按提交时间倒序，第一条就是最新的）
            const latestApproved = approvedFinalSubmissions[0]
            this.approvedSubmission = {
              submitterName: latestApproved.submitter?.name || latestApproved.submitter?.username || '未知',
              submittedAt: latestApproved.submissionTime,
              content: latestApproved.submissionContent || '无',
              attachments: latestApproved.attachmentUrls || []
            }
            console.log('[fetchApprovedSubmission] 找到已批准提交:', this.approvedSubmission)
            console.log('[fetchApprovedSubmission] 原始数据:', latestApproved)
          } else {
            console.log('[fetchApprovedSubmission] 没有找到已批准的最终提交')
          }
        }
      } catch (error) {
        console.error('获取提交信息失败:', error)
      }
    },
    /**
     * 获取审核状态显示文本
     */
    getApprovalStatus(task) {
      if (!task.hasSubmission) {
        return '未提交'
      }
      
      // 优先使用 hasApprovedSubmission 字段判断
      if (task.hasApprovedSubmission) {
        return '已批准'
      }
      
      // 如果有提交但没有批准，检查任务状态
      if (task.status === '待审核' || task.status_value === 'PENDING_REVIEW') {
        return '待审核'
      }
      if (task.status === '完成' || task.status_value === 'DONE') {
        return '已批准'
      }
      
      // 有提交但状态未知
      if (task.hasFinalSubmission) {
        return '待审核'
      }
      
      return '未提交'
    },
    /**
     * 格式化时间
     */
    formatTime(timestamp) {
      if (!timestamp) return '未知'
      try {
        const date = new Date(timestamp)
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (e) {
        return '未知'
      }
    },
    /**
     * 从URL中提取文件名
     */
    getFileNameFromUrl(url) {
      if (!url) return '未知文件'
      try {
        // 从URL中提取文件名（最后一个/后面的部分）
        const parts = url.split('/')
        const filename = parts[parts.length - 1]
        // 解码URL编码的文件名
        return decodeURIComponent(filename)
      } catch (e) {
        return url
      }
    },
    goToTaskList() {
      // 跳转到任务列表页面
      console.log('手动跳转到任务列表页面，项目ID:', this.$route.params.id)
      this.$router.push(`/project/${this.$route.params.id}/tasks`)
    },
    /**
     * 从后端API加载项目任务数据
     */
    async loadProjectTasks() {
      const projectId = this.$route.params.id;
      if (!projectId) {
        console.warn('项目ID不存在，无法加载任务');
        return;
      }
      
      // 设置加载状态
      this.isLoadingTasks = true;
      
      try {
        // 导入任务API
        const { taskAPI } = await import('@/api/task')
        console.log(`[任务加载] 开始获取项目${projectId}的任务列表`);
        
        // 获取所有任务（限制最大1000条）
        const response = await taskAPI.getProjectTasks(projectId, 0, 1000, {
          fields: 'id,title,status,assignees,dueDate,priority,isMilestone,createdAt,createdBy,creatorName,requiredPeople'
        });
        
        if (response?.code === 200 && response.data) {
          const tasksData = response.data;
          // 处理分页数据
          const taskList = Array.isArray(tasksData) ? tasksData : 
                         (tasksData.content || []);
                          
          console.log(`[任务加载] 成功获取到${taskList.length}个任务`);
          // 转换任务数据格式，优先使用后端返回的创建人信息，如果没有则使用本地用户信息
          const currentUserId = this.getCurrentUserId()
          const currentUserName = this.getCurrentUserName()
          this.tasks = taskList.map(task => {
            // 解析执行者信息（后端返回的是TaskDetailDTO.assignees数组）
            let assigneeIds = []
            let assigneeNames = ''
            if (task.assignees && Array.isArray(task.assignees) && task.assignees.length > 0) {
              // 后端返回的是 assignees: [{userId, userName, email, avatarUrl}]
              // 保持userId为字符串类型，避免精度丢失
              assigneeIds = task.assignees.map(a => String(a.userId))
              assigneeNames = task.assignees.map(a => a.userName).join(', ')
            }
            return {
              id: task.id,
              title: task.title,
              description: task.description || '',
              date: task.dueDate || task.due_date || '',
              due_date: task.dueDate || task.due_date,
              dueDate: task.dueDate || task.due_date,
              priority: this.getPriorityDisplay(task.priority || 'MEDIUM'),
              priority_value: task.priority || 'MEDIUM',
              status: this.getStatusDisplay(task.status || 'TODO'),
              status_value: task.status || 'TODO',
              assignee_id: assigneeIds,
              assignee_name: assigneeNames,
              assignees: task.assignees || [], // 保存原始的assignees数组
              participantCount: task.requiredPeople || null, // 从后端获取requiredPeople字段
              created_by: task.createdBy || currentUserId,
              // 如果后端返回的创建人是"未知用户"（auth服务不可用），使用本地用户信息
              created_by_name: task.creatorName === '未知用户' ? currentUserName : (task.creatorName || currentUserName),
              showStatusMenu: false, // 初始化状态菜单为关闭
              // 如果任务状态是"待审核"，说明已经有提交了
              hasSubmission: task.status === 'PENDING_REVIEW' || this.getStatusDisplay(task.status || 'TODO') === '待审核' || task.hasSubmission || false,
              // 是否为里程碑任务
              isMilestone: task.isMilestone || false
            }
          })
          console.log('[loadProjectTasks] 转换后的任务数据:', this.tasks)
          
          // 批量更新任务状态（只检查待审核的任务）
          if (taskList.length > 0) {
            console.log(`[任务加载] 开始批量更新${taskList.length}个任务的状态`);
            // 只获取待审核任务的ID
            const pendingTaskIds = taskList
              .filter(task => task.status === '待审核' || task.status_value === 'PENDING_REVIEW')
              .map(task => task.id);
              
            if (pendingTaskIds.length > 0) {
              console.log(`[任务加载] 发现${pendingTaskIds.length}个待审核任务，开始批量更新`);
              await this.batchUpdateTaskStatus(pendingTaskIds);
            } else {
              console.log('[任务加载] 没有待审核的任务需要更新');
            }
          }
          
          // 更新本地存储并完成加载
          this.saveProjectData();
          this.isLoadingTasks = false;
        } else {
          console.warn('[loadProjectTasks] API返回数据格式异常:', response);
          // 如果API返回失败，尝试从localStorage加载
          this.loadTasksFromLocalStorage();
          this.isLoadingTasks = false;
        }
      } catch (error) {
        console.error('[loadProjectTasks] 加载任务失败:', error);
        // 发生错误时，尝试从localStorage加载
        this.loadTasksFromLocalStorage();
        this.isLoadingTasks = false;
      }
    },
    /**
     * 从localStorage加载任务数据（作为后备方案）
     */
    loadTasksFromLocalStorage() {
      const projectId = this.$route.params.id
      const savedProjects = localStorage.getItem('projects')
      if (savedProjects) {
        const projects = JSON.parse(savedProjects)
        const foundProject = projects.find(p => String(p.id) === String(projectId))
        if (foundProject && foundProject.tasks) {
          // 从localStorage加载任务（已简化日志）
          this.tasks = (foundProject.tasks || []).map(task => ({
            id: task.id,
            title: task.title,
            description: task.description,
            date: task.due_date || task.dueDate || task.date || '',
            due_date: task.due_date || task.dueDate,
            priority: this.getPriorityDisplay(task.priority || 'MEDIUM'),
            priority_value: task.priority || 'MEDIUM',
            status: this.getStatusDisplay(task.status_value || task.status || 'ONGOING'),
            status_value: task.status_value || task.status || 'ONGOING',
            assignee_id: task.assignee_id || [],
            created_by: task.created_by || 1,
            created_by_name: this.getUserNameById(task.created_by || 1),
            showStatusMenu: false
          }))
        }
      }
    },
    /**
     * 检查管理员权限
     * 注意：如果团队成员列表已加载，优先从成员列表判断权限（更可靠）
     */
    async checkAdminPermission() {
      const projectId = this.$route.params.id
      if (!projectId) return
      // 如果团队成员列表已加载，优先从成员列表判断权限
      const currentUserId = this.getCurrentUserId()
      if (currentUserId && this.teamMembers && this.teamMembers.length > 0) {
        const currentMember = this.teamMembers.find(m => {
          const memberId = String(m.id || m.userId || '')
          const userId = String(currentUserId)
          return memberId === userId || String(m.userId) === userId
        })
        if (currentMember) {
          const roleCode = String(currentMember.roleCode || '').toUpperCase()
          const roleName = String(currentMember.role || '').toUpperCase()
          // 从成员列表判断权限
          const isOwnerRole = roleCode === 'OWNER' || 
                             roleName.includes('OWNER') || 
                             roleName.includes('拥有者') || 
                             roleName.includes('负责人') ||
                             currentMember.role === '项目拥有者' ||
                             currentMember.role === '项目负责人'
          const isAdminRole = roleCode === 'ADMIN' || 
                             roleName.includes('ADMIN') || 
                             roleName.includes('管理员') ||
                             currentMember.role === '项目管理员'
          if (isOwnerRole) {
            // 使用 Vue.set 确保响应式更新
            this.$set(this, 'isOwner', true)
            this.$set(this, 'isAdmin', true)
            console.log('[checkAdminPermission] 从成员列表判断为OWNER，跳过API检查', {
              isAdmin: this.isAdmin,
              isOwner: this.isOwner,
              isProjectManager: this.isProjectManager
            })
            // 强制更新视图
            this.$nextTick(() => this.$forceUpdate())
            return // 如果从成员列表判断成功，不再调用API
          } else if (isAdminRole) {
            // 使用 Vue.set 确保响应式更新
            this.$set(this, 'isAdmin', true)
            this.$set(this, 'isOwner', false)
            console.log('[checkAdminPermission] 从成员列表判断为ADMIN，跳过API检查', {
              isAdmin: this.isAdmin,
              isOwner: this.isOwner,
              isProjectManager: this.isProjectManager
            })
            // 强制更新视图
            this.$nextTick(() => this.$forceUpdate())
            return // 如果从成员列表判断成功，不再调用API
          }
        }
      }
      // 如果从成员列表无法判断，调用API检查
      try {
        const { projectAPI } = await import('@/api/project')
        // 并行检查管理员和拥有者身份
        const [adminResponse, ownerResponse] = await Promise.all([
          projectAPI.checkAdmin(projectId),
          projectAPI.checkOwner(projectId)
        ])
        if (adminResponse && adminResponse.code === 200) {
          this.$set(this, 'isAdmin', adminResponse.data === true)
        }
        if (ownerResponse && ownerResponse.code === 200) {
          this.$set(this, 'isOwner', ownerResponse.data === true)
          // 如果API返回是OWNER，也设置isAdmin为true
          if (this.isOwner) {
            this.$set(this, 'isAdmin', true)
          }
        }
        // 强制更新视图
        this.$nextTick(() => {
          this.$forceUpdate()
        })
        console.log('[checkAdminPermission] API权限检查结果:', {
          isAdmin: this.isAdmin,
          isOwner: this.isOwner,
          isProjectManager: this.isProjectManager,
          teamMembersCount: this.teamMembers?.length || 0,
          currentUserId: this.getCurrentUserId()
        })
      } catch (error) {
        console.error('检查管理员权限失败:', error)
        // 如果API检查失败，尝试从团队成员列表判断
        if (currentUserId && this.teamMembers && this.teamMembers.length > 0) {
          const currentMember = this.teamMembers.find(m => {
            const memberId = String(m.id || m.userId || '')
            const userId = String(currentUserId)
            return memberId === userId || String(m.userId) === userId
          })
          if (currentMember) {
            const roleCode = String(currentMember.roleCode || '').toUpperCase()
            const roleName = String(currentMember.role || '').toUpperCase()
            this.isOwner = roleCode === 'OWNER' || roleName.includes('OWNER') || roleName.includes('拥有者') || roleName.includes('负责人') || currentMember.role === '项目拥有者'
            if (this.isOwner) {
              this.isAdmin = true
            } else {
              this.isAdmin = roleCode === 'ADMIN' || roleName.includes('ADMIN') || roleName.includes('管理员') || currentMember.role === '项目管理员'
            }
          }
        }
      }
    },
    async loadProject(projectIdOverride) {
      const projectId = projectIdOverride || this.$route.params.id
      // 先尝试从缓存加载，立即显示
      try {
        const cachedProject = localStorage.getItem(`project_detail_${projectId}`)
        if (cachedProject) {
          const parsed = JSON.parse(cachedProject)
          // 检查缓存是否过期（3分钟）
          if (parsed.timestamp && Date.now() - parsed.timestamp < 3 * 60 * 1000) {
            // 规范化并尝试使用封面缓存
            const rawProject = parsed.data.project || null
            let normalizedUrl = null
            let finalImage = null
            if (rawProject) {
              normalizedUrl = normalizeProjectCoverUrl(rawProject.imageUrl || rawProject.image)
              if (normalizedUrl) {
                const cachedCover = getCachedProjectCover(rawProject.id, normalizedUrl)
                finalImage = cachedCover && cachedCover.dataUrl ? cachedCover.dataUrl : normalizedUrl
              }
            }

            this.project = rawProject
              ? {
                  ...rawProject,
                  image: finalImage || rawProject.image,
                  imageUrl: normalizedUrl || rawProject.imageUrl || rawProject.image
                }
              : null
            this.teamMembers = parsed.data.teamMembers || []
            this.tasks = parsed.data.tasks || []
            this.isLoading = false
            // 使用缓存数据时也提前预加载项目图片和头像
            this.preloadDetailImages()
            // 后台静默刷新封面缓存
            try {
              const proj = this.project
              if (proj && proj.id && proj.imageUrl) {
                cacheProjectCoverIfNeeded(proj.id, proj.imageUrl).catch(() => {})
              }
            } catch (e) {}
            // 后台更新数据（包括团队成员）
            this.loadProjectFromAPI().then(() => {
              // 在数据加载完成后再检查权限
              this.checkAdminPermission()
            }).catch(error => {
              console.error('后台更新项目数据失败:', error)
              // 即使出错也尝试检查权限
              this.checkAdminPermission()
            })
            return
          }
        }
      } catch (e) {
        // 缓存读取失败，继续后续逻辑
      }

      // 如果没有可用的详情缓存，优先尝试使用项目广场列表缓存，立即渲染基础信息
      try {
        const savedProjects = localStorage.getItem('projects')
        if (savedProjects) {
          const projects = JSON.parse(savedProjects)
          const foundProject = projects.find(p => String(p.id) === String(projectId))
          if (foundProject) {
            // 规范化封面 URL 并尝试使用 dataURL 缓存
            const normalizedImageUrl = normalizeProjectCoverUrl(foundProject.imageUrl || foundProject.image)
            const cachedCover = normalizedImageUrl
              ? getCachedProjectCover(foundProject.id, normalizedImageUrl)
              : null
            const finalImage = cachedCover && cachedCover.dataUrl ? cachedCover.dataUrl : normalizedImageUrl

            this.project = {
              id: foundProject.id,
              name: foundProject.name || foundProject.title,
              title: foundProject.title || foundProject.name,
              description: foundProject.description || foundProject.dataAssets || foundProject.direction || '暂无描述',
              startDate: foundProject.startDate || foundProject.start_date || '',
              endDate: foundProject.endDate || foundProject.end_date || '',
              period: (foundProject.start_date || foundProject.startDate) && (foundProject.end_date || foundProject.endDate) ? 
                `${foundProject.start_date || foundProject.startDate} 至 ${foundProject.end_date || foundProject.endDate}` : 
                '未设置',
              status: this.getStatusValue(foundProject.status),
              visibility: foundProject.visibility || 'PRIVATE',
              imageUrl: normalizedImageUrl || getDefaultProjectImage('Project Image'),
              image: finalImage || normalizedImageUrl,
              manager: foundProject.creatorName || '未知',
              teamSize: foundProject.teamSize,
              category: foundProject.category,
              aiCore: foundProject.aiCore,
              tags: foundProject.tags || [],
              tasks: foundProject.tasks || [],
              created_by: foundProject.created_by || 1,
              creatorName: foundProject.creatorName || '未知'
            }
            this.projectStatus = this.project.status || null
            this.isArchived = this.projectStatus === 'ARCHIVED' || this.projectStatus === '已归档'
            // 立即使用列表缓存中的团队成员和邀请槽
            this.teamMembers = foundProject.teamMembers || []
            this.inviteSlots = foundProject.inviteSlots || []

            this.isLoading = false
            // 预加载当前详情需要的图片
            this.preloadDetailImages()
            // 后台更新完整详情和权限
            this.loadProjectFromAPI().then(() => {
              this.checkAdminPermission()
            }).catch(error => {
              console.error('后台更新项目数据失败:', error)
              this.checkAdminPermission()
            })
            return
          }
        }
      } catch (e) {
        // 使用列表缓存失败时，继续从API加载
      }

      // 优先从后端API获取最新的项目数据
      try {
        const { projectAPI } = await import('@/api/project')
        const response = await projectAPI.getProjectById(projectId)
        if (response && response.code === 200 && response.data) {
          const apiProject = response.data
          
          // 调试：检查后端返回的项目数据
          console.log('[ProjectDetail] 后端返回的项目数据:', {
            projectId: apiProject.id,
            projectName: apiProject.name,
            creatorId: apiProject.creatorId,
            creatorName: apiProject.creatorName,
            完整数据: apiProject
          })
          
          // 使用API返回的最新数据
          let detailImageUrl = normalizeProjectCoverUrl(apiProject.imageUrl)
          const cachedCover = getCachedProjectCover(apiProject.id, detailImageUrl)
          const finalImage = cachedCover && cachedCover.dataUrl ? cachedCover.dataUrl : detailImageUrl || getDefaultProjectImage('Project Image')

          this.project = {
            id: apiProject.id,
            name: apiProject.name,
            title: apiProject.name,
            description: apiProject.description || '暂无描述',
            startDate: apiProject.startDate || '',
            endDate: apiProject.endDate || '',
            period: (apiProject.startDate && apiProject.endDate) ? 
              `${apiProject.startDate} 至 ${apiProject.endDate}` : 
              '未设置',
            status: apiProject.status || 'PLANNING',
            visibility: apiProject.visibility || 'PRIVATE',
            imageUrl: detailImageUrl || getDefaultProjectImage('Project Image'),
            image: finalImage,
            manager: apiProject.creatorName || '未知', // 使用项目的创建者名称作为负责人
            teamSize: apiProject.teamSize || 1,
            category: apiProject.category || '其他',
            aiCore: '待定',
            tags: apiProject.tags || [],
            tasks: [],
            created_by: apiProject.creatorId || 1,
            creatorName: apiProject.creatorName || '未知' // 保存创建者名称
          }
          this.projectStatus = apiProject.status || null
          this.isArchived = this.projectStatus === 'ARCHIVED'
          // 并行加载团队成员和任务数据，提升加载速度
          this.isLoading = false
          Promise.all([
            this.loadTeamMembers(),
            this.loadProjectTasks()
          ]).then(() => {
            // 加载完团队成员后，更新负责人为项目拥有者（如果有的话）
            this.updateManagerFromTeamMembers()
          // 检查权限（需要在团队成员加载完成后）
          // 延迟一点检查权限，确保团队成员数据已更新
          this.$nextTick(() => {
            this.checkAdminPermission()
          })
            // 保存到缓存
            this.saveProjectDetailCache()
          }).catch(error => {
            console.error('并行加载数据时出错:', error)
          })
          return
        }
      } catch (error) {
        console.error('从API加载项目失败，回退到localStorage:', error)
      }
      // 如果API失败，从localStorage加载项目数据（作为后备）
      const savedProjects = localStorage.getItem('projects')
      if (savedProjects) {
        const projects = JSON.parse(savedProjects)
        // 使用字符串比较，因为后端返回的是字符串ID
        const foundProject = projects.find(p => String(p.id) === String(projectId))
        if (foundProject) {
          // 将项目广场的数据格式转换为详情页面的格式
          this.project = {
            id: foundProject.id,
            name: foundProject.name || foundProject.title,
            title: foundProject.title || foundProject.name,
            description: foundProject.description || foundProject.dataAssets || foundProject.direction || '暂无描述',
            startDate: foundProject.startDate || foundProject.start_date || '',
            endDate: foundProject.endDate || foundProject.end_date || '',
            period: (foundProject.start_date || foundProject.startDate) && (foundProject.end_date || foundProject.endDate) ? 
              `${foundProject.start_date || foundProject.startDate} 至 ${foundProject.end_date || foundProject.endDate}` : 
              '未设置',
            status: this.getStatusValue(foundProject.status),
            visibility: foundProject.visibility || 'PRIVATE',
            imageUrl: normalizeProjectCoverUrl(foundProject.imageUrl || foundProject.image) || getDefaultProjectImage('Project Image'),
            image: normalizeProjectCoverUrl(foundProject.image || foundProject.imageUrl),
            manager: foundProject.creatorName || '未知',
            teamSize: foundProject.teamSize,
            category: foundProject.category,
            aiCore: foundProject.aiCore,
            tags: foundProject.tags || [],
            tasks: foundProject.tasks || [],
            created_by: foundProject.created_by || 1,
            creatorName: foundProject.creatorName || '未知'
          }
          this.projectStatus = this.project.status || null
          this.isArchived = this.projectStatus === 'ARCHIVED' || this.projectStatus === '已归档'
          // 加载团队成员数据
          this.teamMembers = foundProject.teamMembers || []
          this.inviteSlots = foundProject.inviteSlots || []
          // 从团队成员中查找项目拥有者，更新负责人
          this.updateManagerFromTeamMembers()
          // 并行加载任务和团队成员数据
          this.isLoading = false
          Promise.all([
            this.loadProjectTasks(),
            this.loadTeamMembers()
          ]).then(() => {
            this.updateManagerFromTeamMembers()
            this.saveProjectDetailCache()
            // 使用项目广场回退数据时，同样预加载当前详情需要的图片
            this.preloadDetailImages()
          }).catch(error => {
            console.error('并行加载数据时出错:', error)
          })
        } else {
          this.project = {
            id: projectId,
            title: '项目不存在',
            description: '抱歉，未找到指定的项目',
            period: '未知',
            status: '未知',
            manager: '未知'
          }
          this.isLoading = false
        }
      } else {
        this.project = {
          id: projectId,
          title: '项目不存在',
          description: '抱歉，未找到指定的项目',
          period: '未知',
          status: '未知',
          manager: '未知'
        }
        this.isLoading = false
      }
    },
    async loadProjectFromAPI() {
      // 后台更新项目数据
      try {
        const { projectAPI } = await import('@/api/project')
        const projectId = this.$route.params.id
        const response = await projectAPI.getProjectById(projectId)
        if (response && response.code === 200 && response.data) {
          const apiProject = response.data
          
          // 调试：检查后端返回的项目数据
          console.log('[ProjectDetail] 后端返回的项目数据:', {
            projectId: apiProject.id,
            projectName: apiProject.name,
            creatorId: apiProject.creatorId,
            creatorName: apiProject.creatorName,
            完整数据: apiProject
          })
          
          // 使用API返回的最新数据
          let detailImageUrl = normalizeProjectCoverUrl(apiProject.imageUrl)
          const cachedCover = getCachedProjectCover(apiProject.id, detailImageUrl)
          const finalImage = cachedCover && cachedCover.dataUrl ? cachedCover.dataUrl : detailImageUrl || getDefaultProjectImage('Project Image')

          this.project = {
            id: apiProject.id,
            name: apiProject.name,
            title: apiProject.name,
            description: apiProject.description || '暂无描述',
            startDate: apiProject.startDate || '',
            endDate: apiProject.endDate || '',
            period: (apiProject.startDate && apiProject.endDate) ? 
              `${apiProject.startDate} 至 ${apiProject.endDate}` : 
              '未设置',
            status: apiProject.status || 'PLANNING',
            visibility: apiProject.visibility || 'PRIVATE',
            imageUrl: detailImageUrl || getDefaultProjectImage('Project Image'),
            image: finalImage,
            manager: apiProject.creatorName || '未知', // 使用项目的创建者名称作为负责人
            teamSize: apiProject.teamSize || 1,
            category: apiProject.category || '其他',
            aiCore: '待定',
            tags: apiProject.tags || [],
            tasks: [],
            created_by: apiProject.creatorId || 1,
            creatorName: apiProject.creatorName || '未知'
          }
          this.projectStatus = apiProject.status || null
          this.isArchived = this.projectStatus === 'ARCHIVED'
          // 并行加载团队成员和任务数据
          await Promise.all([
            this.loadTeamMembers(),
            this.loadProjectTasks()
          ])
          this.updateManagerFromTeamMembers()
          this.saveProjectDetailCache()
          // 后台刷新项目数据完成后，再次预加载图片，确保使用最新URL
          this.preloadDetailImages()
        }
      } catch (error) {
        console.error('后台更新项目数据失败:', error)
      }
    },
    /**
     * 预加载项目详情页中会用到的图片（项目大图 + 各类头像）
     */
    preloadDetailImages() {
      try {
        const urls = []

        if (this.project) {
          if (this.project.imageUrl) {
            urls.push(this.project.imageUrl)
          } else if (this.project.image) {
            urls.push(this.project.image)
          }
        }

        if (Array.isArray(this.teamMembers)) {
          this.teamMembers.forEach(m => {
            if (m && m.avatar) {
              urls.push(m.avatar)
            }
          })
        }

        if (Array.isArray(this.tasks)) {
          this.tasks.forEach(task => {
            if (Array.isArray(task.assignees)) {
              task.assignees.forEach(a => {
                if (a && a.avatarUrl) {
                  urls.push(a.avatarUrl)
                }
              })
            }
          })
        }

        const uniqueUrls = Array.from(new Set(urls.filter(Boolean)))
        if (!uniqueUrls.length) return

        preloadImages(uniqueUrls).catch(() => {
          // 预加载失败不会影响正常渲染
        })
      } catch (e) {
        // 任何异常都不影响页面正常使用
      }
    },
    saveProjectDetailCache() {
      // 保存项目详情到缓存
      try {
        const projectId = this.$route.params.id
        localStorage.setItem(`project_detail_${projectId}`, JSON.stringify({
          data: {
            project: this.project,
            teamMembers: this.teamMembers,
            tasks: this.tasks
          },
          timestamp: Date.now()
        }))
      } catch (e) {
        // 忽略缓存写入错误
      }
    },
    goToUserProfile(member) {
      // 从成员对象中取 userId 或 id
      const userId = member.userId || member.id
      if (!userId) return
      // 目前个人信息页路由为 /profile，可通过查询参数传入要查看的用户ID
      // 同时将头像一并传过去，作为他人页面头像的兜底展示
      this.$router.push({
        path: '/profile',
        query: {
          userId: String(userId),
          avatar: member.avatar || ''
        }
      })
    },
    loadTeamMembersFromLocalStorage() {
      const projectId = this.$route.params.id
      const savedProjects = localStorage.getItem('projects')
      if (savedProjects) {
        const projects = JSON.parse(savedProjects)
        const foundProject = projects.find(p => String(p.id) === String(projectId))
        if (foundProject) {
          this.teamMembers = foundProject.teamMembers || [
            { id: 1, name: this.getCurrentUserName(), role: '项目负责人', avatar: null }
          ]
          this.inviteSlots = foundProject.inviteSlots || []
        }
      }
      // 如果没有找到，使用默认值
      if (!this.teamMembers || this.teamMembers.length === 0) {
        this.teamMembers = [
          { id: 1, name: this.getCurrentUserName(), role: '项目负责人', avatar: null }
        ]
      }
    },
    goBack() {
      this.$router.go(-1)
    },
    goToProjectDashboard() {
      const projectId = this.$route.params.id
      if (projectId) {
        this.$router.push(`/project-dashboard/${projectId}`)
      }
    },
    goToProjectKnowledge() {
      const projectId = this.$route.params.id || this.project?.id
      console.log('跳转到项目知识库，项目ID:', projectId, '路由路径:', `/project-knowledge/${projectId}`)
      if (!projectId) {
        console.error('项目ID为空，无法跳转')
        alert('项目ID无效，无法跳转到知识库')
        return
      }
      // 使用路由名称跳转，更可靠
      this.$router.push({
        name: 'ProjectKnowledge',
        params: { id: String(projectId) }
      }).catch(err => {
        // 如果路由名称失败，回退到路径跳转
        if (err.name !== 'NavigationDuplicated') {
          console.error('路由跳转失败:', err)
          this.$router.push(`/project-knowledge/${projectId}`).catch(e => {
            console.error('路径跳转也失败:', e)
          })
        }
      })
    },
    goToAIAssistant() {
      const projectId = this.project?.id || this.$route.params.id
      this.$router.push({ path: '/ai-assistant', query: { projectId } })
    },
    goToOperationLog() {
      const projectId = this.project?.id || this.$route.params.id
      if (!projectId) {
        console.error('项目ID为空，无法跳转')
        alert('项目ID无效，无法跳转到操作日志')
        return
      }
      this.$router.push({
        name: 'ProjectOperationLog',
        params: { id: String(projectId) }
      }).catch(err => {
        if (err.name !== 'NavigationDuplicated') {
          console.error('路由跳转失败:', err)
          this.$router.push(`/project-operation-log/${projectId}`).catch(e => {
            console.error('路径跳转也失败:', e)
          })
        }
      })
    },
    addTeamMember() {
      const name = prompt('请输入成员姓名:')
      if (name && name.trim()) {
        const role = prompt('请输入成员角色:')
        const newMember = {
          id: Date.now(),
          name: name.trim(),
          role: role ? role.trim() : '团队成员',
          avatar: null
        }
        this.teamMembers.push(newMember)
        this.saveProjectData()
        alert('成员添加成功！')
      }
    },
    inviteMember() {
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能邀请成员')
        return
      }
      // 打开邀请成员弹窗
      this.inviteMemberModalOpen = true
      this.userSearchKeyword = ''
      this.searchedUsers = []
      this.selectedUserIds = [] // 清空选中的用户列表
      this.hasSearched = false
    },
    closeInviteMemberModal() {
      this.inviteMemberModalOpen = false
      this.userSearchKeyword = ''
      this.searchedUsers = []
      this.selectedUserIds = [] // 清空选中的用户列表
      this.hasSearched = false
      this.isSearching = false
      this.isInviting = false
      this.displayedUserCount = 4 // 重置显示数量
      if (this.searchDebounceTimer) {
        clearTimeout(this.searchDebounceTimer)
        this.searchDebounceTimer = null
      }
    },
    handleSearchInput() {
      // 防抖搜索：用户停止输入300ms后自动搜索
      if (this.searchDebounceTimer) {
        clearTimeout(this.searchDebounceTimer)
      }
      if (this.userSearchKeyword.trim()) {
        this.searchDebounceTimer = setTimeout(() => {
          this.searchUsers()
        }, 300)
      } else {
        this.searchedUsers = []
        this.hasSearched = false
      }
    },
    async searchUsers() {
      if (!this.userSearchKeyword.trim()) {
        this.showSuccessToast('请输入搜索关键词')
        return
      }
      this.isSearching = true
      this.hasSearched = false
      this.displayedUserCount = 4 // 重置显示数量
      try {
        const { projectAPI } = await import('@/api/project')
        const response = await projectAPI.searchUsers(this.userSearchKeyword.trim(), 0, 20)
        console.log('搜索用户响应:', response)
        if (response && response.code === 200) {
          // 处理分页数据或直接的用户列表
          if (response.data && response.data.content) {
            this.searchedUsers = response.data.content
          } else if (Array.isArray(response.data)) {
            this.searchedUsers = response.data
          } else {
            this.searchedUsers = []
          }
          this.hasSearched = true
          console.log('搜索到的用户:', this.searchedUsers)
        } else {
          this.showSuccessToast(response?.msg || '搜索失败')
          this.searchedUsers = []
          this.hasSearched = true
        }
      } catch (error) {
        console.error('搜索用户失败:', error)
        this.showSuccessToast('搜索用户失败: ' + (error.message || '网络错误'))
        this.searchedUsers = []
        this.hasSearched = true
      } finally {
        this.isSearching = false
      }
    },
    maskEmail(email) {
      if (!email || typeof email !== 'string') return ''
      const [local, domain] = email.split('@')
      if (!domain) {
        return email
      }
      if (!local || local.length <= 2) {
        return '*@' + domain
      }
      const start = local.slice(0, 2)
      const end = local.slice(-2)
      const stars = '*'.repeat(Math.max(local.length - 4, 1))
      return `${start}${stars}${end}@${domain}`
    },
    selectUser(user) {
      // 后端返回的字段是 id，不是 userId
      const userId = user.id || user.userId
      const index = this.selectedUserIds.indexOf(userId)
      if (index > -1) {
        // 已选中，则取消选中
        this.selectedUserIds.splice(index, 1)
        console.log('取消选中用户:', user.name, '当前已选:', this.selectedUserIds.length)
      } else {
        // 未选中，则添加到选中列表
        this.selectedUserIds.push(userId)
        console.log('选中用户:', user.name, '当前已选:', this.selectedUserIds.length)
      }
    },
    loadMoreUsers() {
      // 每次点击"更多"按钮，增加4个用户
      this.displayedUserCount += 4
    },
    async confirmInvite() {
      if (this.selectedUserIds.length === 0) {
        this.showSuccessToast('请先选择要邀请的用户')
        return
      }
      this.isInviting = true
      try {
        const { projectAPI } = await import('@/api/project')
        const projectId = this.$route.params.id
        // 批量邀请用户
        let successCount = 0
        let failCount = 0
        for (const userId of this.selectedUserIds) {
          try {
            // ADMIN不能将新成员设置为OWNER，只能设置为MEMBER或ADMIN
            // 但只有OWNER可以将新成员设置为ADMIN，ADMIN只能设置为MEMBER
            const defaultRole = this.isProjectOwner ? 'MEMBER' : 'MEMBER' // 默认角色为普通成员
            const response = await projectAPI.inviteMember(projectId, {
              userId: userId,
              role: defaultRole
            })
            if (response && response.code === 200) {
              successCount++
            } else {
              failCount++
              console.error('邀请用户失败:', userId, response?.msg)
            }
          } catch (error) {
            failCount++
            console.error('邀请用户异常:', userId, error)
          }
        }
        // 显示邀请结果
        if (successCount > 0 && failCount === 0) {
          // 使用弹窗提示
          this.$alert('邀请成员成功，等待成员同意', '提示', {
            confirmButtonText: '确定',
            type: 'success',
            center: true
          })
        } else if (successCount > 0 && failCount > 0) {
          this.$alert(`成功邀请 ${successCount} 位，失败 ${failCount} 位`, '提示', {
            confirmButtonText: '确定',
            type: 'warning',
            center: true
          })
        } else {
          this.$alert('邀请失败，请重试', '提示', {
            confirmButtonText: '确定',
            type: 'error',
            center: true
          })
        }
        if (successCount > 0) {
          this.closeInviteMemberModal()
          // 重新加载团队成员列表
          this.loadTeamMembers()
        }
      } catch (error) {
        console.error('邀请成员失败:', error)
        this.$alert('邀请失败: ' + (error.message || '网络错误'), '提示', {
          confirmButtonText: '确定',
          type: 'error',
          center: true
        })
      } finally {
        this.isInviting = false
      }
    },
    async loadTeamMembers() {
      try {
        const { projectAPI } = await import('@/api/project')
        const projectId = this.$route.params.id
        const response = await projectAPI.getProjectMembers(projectId, 0, 50)
        if (response && response.code === 200) {
          console.log('[loadTeamMembers] 后端返回的原始数据:', JSON.stringify(response.data, null, 2))
          // 处理成员数据
          if (response.data && response.data.content) {
            this.teamMembers = response.data.content.map(member => {
              const userId = String(member.userId || member.id || '')
              const mapped = {
                id: userId,  // 使用userId作为id
                userId: userId, // 同时保存userId
              name: member.username || member.name || '未知用户',
              role: member.roleName || member.role || '成员',
                roleCode: member.roleCode || this.getRoleCodeFromName(member.roleName || member.role), // 保存角色代码
              // 先设置为null，后续异步加载头像
              avatar: null
              }
              console.log('[loadTeamMembers] 映射成员数据:', {
                原始: { userId: member.userId, roleCode: member.roleCode, roleName: member.roleName },
                映射后: mapped
              })
              return mapped
            })
            // 异步加载每个成员的头像
            this.loadMemberAvatars()
          } else if (Array.isArray(response.data)) {
            this.teamMembers = response.data.map(member => {
              const userId = String(member.userId || member.id || '')
              const mapped = {
                id: userId,
                userId: userId,
              name: member.username || member.name || '未知用户',
              role: member.roleName || member.role || '成员',
                roleCode: member.roleCode || this.getRoleCodeFromName(member.roleName || member.role),
              // 先设置为null，后续异步加载头像
              avatar: null
              }
              console.log('[loadTeamMembers] 映射成员数据:', {
                原始: { userId: member.userId, roleCode: member.roleCode, roleName: member.roleName },
                映射后: mapped
              })
              return mapped
            })
            // 异步加载每个成员的头像
            this.loadMemberAvatars()
          }
          const currentUserId = this.getCurrentUserId()
          console.log('[loadTeamMembers] 团队成员加载完成:', {
            memberCount: this.teamMembers.length,
            members: this.teamMembers.map(m => ({
              id: m.id,
              userId: m.userId,
              name: m.name,
              role: m.role,
              roleCode: m.roleCode
            })),
            currentUserId: currentUserId,
            currentUserIdType: typeof currentUserId,
            isProjectManager: this.isProjectManager,
            isProjectOwner: this.isProjectOwner,
            isAdmin: this.isAdmin,
            isOwner: this.isOwner
          })
          // 立即从团队成员列表更新权限状态（不等待API）
          // 使用 Vue.set 确保响应式更新
          if (currentUserId) {
            const currentUserIdStr = String(currentUserId)
            console.log('[loadTeamMembers] 查找当前用户:', {
              currentUserId: currentUserIdStr,
              currentUserIdType: typeof currentUserId,
              teamMembers: this.teamMembers.map(m => ({
                id: m.id,
                userId: m.userId,
                name: m.name,
                role: m.role,
                roleCode: m.roleCode,
                idType: typeof m.id,
                userIdType: typeof m.userId
              }))
            })
            const currentMember = this.teamMembers.find(m => {
              // 尝试多种匹配方式
              const memberId = String(m.id || '')
              const memberUserId = String(m.userId || '')
              const userId = String(currentUserId)
              const match = memberId === userId || memberUserId === userId
              if (match) {
                console.log('[loadTeamMembers] ✅ 找到当前用户成员:', {
                  memberId,
                  memberUserId,
                  userId,
                  member: {
                    id: m.id,
                    userId: m.userId,
                    name: m.name,
                    role: m.role,
                    roleCode: m.roleCode
                  }
                })
              }
              return match
            })
            if (currentMember) {
              const roleCode = String(currentMember.roleCode || '').toUpperCase()
              const roleName = String(currentMember.role || '').toUpperCase()
              console.log('[loadTeamMembers] 当前成员角色信息:', {
                roleCode,
                roleName,
                originalRole: currentMember.role,
                originalRoleCode: currentMember.roleCode,
                fullMember: currentMember
              })
              // 直接更新数据属性（Vue 2的响应式系统会自动处理）
              // 检查角色代码和角色名称
              const isOwnerRole = roleCode === 'OWNER' || 
                                 roleName.includes('OWNER') || 
                                 roleName.includes('拥有者') || 
                                 roleName.includes('负责人') ||
                                 currentMember.role === '项目拥有者' ||
                                 currentMember.role === '项目负责人'
              const isAdminRole = roleCode === 'ADMIN' || 
                                 roleName.includes('ADMIN') || 
                                 roleName.includes('管理员') ||
                                 currentMember.role === '项目管理员'
              if (isOwnerRole) {
                // 使用 Vue.set 确保响应式更新
                this.$set(this, 'isOwner', true)
                this.$set(this, 'isAdmin', true) // OWNER 也是管理员
                console.log('[loadTeamMembers] ✅ 从成员列表检测到当前用户是OWNER，已更新权限状态', {
                  roleCode,
                  roleName,
                  originalRole: currentMember.role,
                  isOwner: this.isOwner,
                  isAdmin: this.isAdmin,
                  isProjectManager: this.isProjectManager
                })
              } else if (isAdminRole) {
                // 使用 Vue.set 确保响应式更新
                this.$set(this, 'isAdmin', true)
                this.$set(this, 'isOwner', false)
                console.log('[loadTeamMembers] ✅ 从成员列表检测到当前用户是ADMIN，已更新权限状态', {
                  roleCode,
                  roleName,
                  originalRole: currentMember.role,
                  isAdmin: this.isAdmin,
                  isOwner: this.isOwner,
                  isProjectManager: this.isProjectManager
                })
              } else {
                console.log('[loadTeamMembers] ⚠️ 当前用户不是管理员，角色:', { 
                  roleCode, 
                  roleName,
                  originalRole: currentMember.role,
                  originalRoleCode: currentMember.roleCode
                })
                // 如果不是管理员，明确设置为 false
                this.$set(this, 'isAdmin', false)
                this.$set(this, 'isOwner', false)
              }
              // 强制触发视图更新，确保按钮显示
              this.$nextTick(() => {
                console.log('[loadTeamMembers] 权限状态更新后（nextTick）:', {
                  isAdmin: this.isAdmin,
                  isOwner: this.isOwner,
                  isProjectManager: this.isProjectManager,
                  computedIsProjectManager: this.isProjectManager
                })
                // 强制更新视图，确保按钮显示
                this.$forceUpdate()
              })
            } else {
              console.warn('[loadTeamMembers] ❌ 未找到当前用户在成员列表中', {
                currentUserId: currentUserIdStr,
                memberIds: this.teamMembers.map(m => ({ 
                  id: m.id, 
                  userId: m.userId,
                  idType: typeof m.id,
                  userIdType: typeof m.userId
                }))
              })
            }
          } else {
            console.warn('[loadTeamMembers] ⚠️ 无法获取当前用户ID，savedUserInfo:', localStorage.getItem('user_info'))
          }
          // 将团队成员数量写入缓存，供项目广场读取显示
          try {
            const cacheKey = `project_member_count_${projectId}`
            localStorage.setItem(cacheKey, String(this.teamMembers.length))
          } catch (e) {
            console.warn('写入成员数量缓存失败:', e?.message || e)
          }
          // 加载完团队成员后，更新负责人为项目拥有者（如果有的话）
          this.updateManagerFromTeamMembers()
          // 确保权限状态已更新后，再次检查并更新视图
          this.$nextTick(() => {
            console.log('[loadTeamMembers] 最终权限检查:', {
              isAdmin: this.isAdmin,
              isOwner: this.isOwner,
              isProjectManager: this.isProjectManager,
              teamMembersCount: this.teamMembers.length,
              currentUserId: this.getCurrentUserId()
            })
            // 强制更新视图，确保按钮显示
            this.$forceUpdate()
            // 再次检查权限状态，如果还没有设置，尝试从成员列表设置
            if (!this.isAdmin && !this.isOwner) {
              const currentUserId = this.getCurrentUserId()
              if (currentUserId && this.teamMembers && this.teamMembers.length > 0) {
                const currentMember = this.teamMembers.find(m => {
                  const memberId = String(m.id || m.userId || '')
                  const userId = String(currentUserId)
                  return memberId === userId || String(m.userId) === userId
                })
                if (currentMember) {
                  const roleCode = String(currentMember.roleCode || '').toUpperCase()
                  const roleName = String(currentMember.role || '').toUpperCase()
                  const isOwnerRole = roleCode === 'OWNER' || roleName.includes('OWNER') || roleName.includes('拥有者') || roleName.includes('负责人') || currentMember.role === '项目拥有者'
                  const isAdminRole = roleCode === 'ADMIN' || roleName.includes('ADMIN') || roleName.includes('管理员') || currentMember.role === '项目管理员'
                  if (isOwnerRole) {
                    this.$set(this, 'isOwner', true)
                    this.$set(this, 'isAdmin', true)
                    this.$forceUpdate()
                  } else if (isAdminRole) {
                    this.$set(this, 'isAdmin', true)
                    this.$set(this, 'isOwner', false)
                    this.$forceUpdate()
                  }
                }
              }
            }
          })
        }
      } catch (error) {
        console.error('加载团队成员失败:', error)
        // 失败时保留原有数据
      }
    },
    /**
     * 异步加载所有成员的头像
     */
    async loadMemberAvatars() {
      const { avatarAPI } = await import('@/api/avatar')
      for (const member of this.teamMembers) {
        if (!member.userId) continue

        // 优先从本次会话的缓存中读取头像URL，避免重复请求
        try {
          const cacheKey = `avatar_cache_${member.userId}`
          const cached = typeof window !== 'undefined' ? window.sessionStorage.getItem(cacheKey) : null
          if (cached) {
            this.$set(member, 'avatar', cached)
            continue
          }
        } catch (e) {
          // sessionStorage 不可用时直接忽略缓存
        }

        try {
          const response = await avatarAPI.getAvatarInfoById(member.userId)
          if (response && response.code === 200 && response.data) {
            const avatarData = response.data
            let avatarUrl = null
            // 优先使用 dataUrl（Base64格式，可直接用于img src）
            if (avatarData.dataUrl) {
              avatarUrl = avatarData.dataUrl
            } else if (avatarData.sizes) {
              avatarUrl = avatarData.sizes.original || avatarData.sizes['256'] || avatarData.sizes['512']
            } else if (avatarData.minio_url || avatarData.minioUrl || avatarData.cdn_url || avatarData.cdnUrl) {
              avatarUrl = avatarData.minio_url || avatarData.minioUrl || avatarData.cdn_url || avatarData.cdnUrl
            }
            if (avatarUrl) {
              this.$set(member, 'avatar', avatarUrl)
              // 将头像URL写入本次会话缓存，后续切换页面直接复用
              try {
                const cacheKey = `avatar_cache_${member.userId}`
                if (typeof window !== 'undefined') {
                  window.sessionStorage.setItem(cacheKey, avatarUrl)
                }
              } catch (e) {
                // 写缓存失败不影响正常显示
              }
              console.log(`[loadMemberAvatars] 加载成员 ${member.name} 头像成功`)
            }
          }
        } catch (error) {
          // 用户可能没有设置头像，忽略错误
          console.log(`[loadMemberAvatars] 成员 ${member.name} 没有头像或加载失败`)
        }
      }
      // 成员头像加载或从缓存恢复后，触发一次图片预加载，提升后续滚动体验
      this.preloadDetailImages()
    },
    handleAvatarUpdated({ userId, avatarUrl }) {
      // 💡 局部更新：只更新该用户的头像，无需重新请求整个成员列表
      console.log('🔔 ProjectDetail收到头像更新事件:', {
        userId,
        userIdType: typeof userId,
        avatarUrl: avatarUrl.substring(0, 50) + '...',
        teamMembersCount: this.teamMembers.length,
        teamMemberIds: this.teamMembers.map(m => ({ id: m.id, type: typeof m.id }))
      })
      // userId 和 member.id 都已经是字符串，直接比较
      const member = this.teamMembers.find(m => m.id === userId)
      if (member) {
        console.log('✅ 找到成员:', member.name)
        console.log('更新前的头像:', member.avatar?.substring(0, 50))
        this.$set(member, 'avatar', avatarUrl)
        // 同步更新本次会话的头像缓存
        try {
          const cacheKey = `avatar_cache_${userId}`
          if (typeof window !== 'undefined') {
            window.sessionStorage.setItem(cacheKey, avatarUrl)
          }
        } catch (e) {
          // 缓存异常不影响界面
        }
        console.log(`✅ 已更新团队成员 ${member.name}(${userId}) 的头像`)
        console.log('更新后的头像:', member.avatar?.substring(0, 50))
        // 强制Vue更新视图
        this.$forceUpdate()
      } else {
        console.warn(`⚠️ 用户 ${userId} 不在当前项目成员列表中`)
        console.warn('当前成员列表:', this.teamMembers.map(m => ({ id: m.id, name: m.name })))
      }
    },
    updateManagerFromTeamMembers() {
      // 从团队成员中查找项目拥有者
      // 可能的角色名称：OWNER, PROJECT_OWNER, 项目负责人, 项目拥有者
      const ownerRoles = ['OWNER', 'PROJECT_OWNER', '项目负责人', '项目拥有者', '负责人']
      const owner = this.teamMembers.find(member => {
        const role = (member.role || '').toUpperCase()
        return ownerRoles.some(ownerRole => role === ownerRole.toUpperCase() || role.includes(ownerRole.toUpperCase()))
      })
      if (owner) {
        // 如果找到了项目拥有者，使用其名称作为负责人
        this.project.manager = owner.name
        console.log('从团队成员中找到项目拥有者:', owner.name, '角色:', owner.role)
      } else if (this.project.creatorName && this.project.creatorName !== '未知') {
        // 如果没找到拥有者，使用创建者名称
        this.project.manager = this.project.creatorName
        console.log('使用项目创建者作为负责人:', this.project.creatorName)
      } else {
        // 如果都没有，保持默认值或显示未知
        console.log('未找到项目拥有者，负责人保持为:', this.project.manager)
      }
    },
    // 打开移除成员确认弹窗
    async removeTeamMember(memberId) {
      // 检查要移除的成员
      const member = this.teamMembers.find(m => String(m.id) === String(memberId))
      if (!member) {
        this.showSuccessToast('未找到该成员')
        return
      }
      // 不能移除项目拥有者
      if (this.isOwnerMember(member)) {
        this.showSuccessToast('不能移除项目拥有者')
        return
      }
      // ADMIN不能移除其他ADMIN，只有OWNER可以
      if (this.isAdminRole(member) && !this.isProjectOwner) {
        this.showSuccessToast('管理员不能移除其他管理员，只有项目拥有者可以')
        return
      }
      this.memberToRemove = member
      this.removeMemberConfirmOpen = true
    },
    // 取消移除成员
    cancelRemoveMember() {
      this.removeMemberConfirmOpen = false
      this.memberToRemove = null
    },
    // 确认移除成员
    async confirmRemoveMember() {
      const member = this.memberToRemove
      if (!member) {
        this.cancelRemoveMember()
        return
      }
      try {
        const { projectAPI } = await import('@/api/project')
        const projectId = this.$route.params.id
        const response = await projectAPI.removeMember(projectId, member.id)
        if (response && response.code === 200) {
          await this.loadTeamMembers()
          this.showSuccessToast('成员已成功移除')
          console.log('成功移除成员:', member.id)
        } else {
          const errorMsg = response?.msg || response?.message || '移除成员失败'
          this.showSuccessToast(errorMsg)
          console.error('移除成员失败:', response)
        }
      } catch (error) {
        console.error('移除成员时出错:', error)
        const errorMsg = error?.msg || error?.message || '网络错误'
        this.showSuccessToast('移除成员失败: ' + errorMsg)
      } finally {
        this.cancelRemoveMember()
      }
    },
    // 打开取消邀请确认弹窗
    removeInviteSlot(slotId) {
      this.inviteSlotToRemove = slotId
      this.removeInviteConfirmOpen = true
    },
    // 取消移除邀请
    cancelRemoveInviteSlot() {
      this.removeInviteConfirmOpen = false
      this.inviteSlotToRemove = null
    },
    // 确认移除邀请
    confirmRemoveInviteSlot() {
      if (!this.inviteSlotToRemove) {
        this.cancelRemoveInviteSlot()
        return
      }
      this.inviteSlots = this.inviteSlots.filter(s => s.id !== this.inviteSlotToRemove)
      this.saveProjectData()
      this.cancelRemoveInviteSlot()
    },
    saveProjectData() {
      // 保存项目数据到localStorage
      const savedProjects = JSON.parse(localStorage.getItem('projects') || '[]')
      console.log('查找项目ID:', this.project.id, '类型:', typeof this.project.id)
      // 使用字符串比较确保ID匹配
      const projectIndex = savedProjects.findIndex(p => String(p.id) === String(this.project.id))
      if (projectIndex !== -1) {
        // 更新项目的所有字段
        savedProjects[projectIndex] = {
          ...savedProjects[projectIndex],
          id: this.project.id, // 确保ID被保存
          name: this.project.name,
          title: this.project.title,
          description: this.project.description,
          startDate: this.project.startDate,
          endDate: this.project.endDate,
          visibility: this.project.visibility,
          status: this.project.status,
          imageUrl: this.project.imageUrl || this.project.image,
          image: this.project.image || this.project.imageUrl,
          teamMembers: this.teamMembers,
          inviteSlots: this.inviteSlots,
          tasks: this.tasks
        }
        localStorage.setItem('projects', JSON.stringify(savedProjects))
        console.log('项目数据已保存到localStorage，项目ID:', this.project.id)
        console.log('保存的任务数据:', this.tasks.map(t => ({ title: t.title, status: t.status, status_value: t.status_value })))
      } else {
        console.log('未找到项目，无法保存数据。项目ID:', this.project.id)
        console.log('现有项目ID列表:', savedProjects.map(p => ({ id: p.id, type: typeof p.id })))
      }
    },
    // 项目操作按钮功能
    editProject() {
      // 初始化编辑数据
      // 确保status是数据库的英文值
      const statusValue = this.getStatusValue(this.project.status)
      this.editProjectData = {
        name: this.project.name || this.project.title,
        description: this.project.description || '',
        startDate: this.project.startDate || '',
        endDate: this.project.endDate || '',
        visibility: this.project.visibility || 'PRIVATE',
        status: statusValue || 'ONGOING',
        imageUrl: this.project.imageUrl || this.project.image || getDefaultProjectImage('Project Image')
      }
      console.log('编辑项目数据初始化:', this.editProjectData)
      this.editProjectModalOpen = true
    },
    closeEditProjectModal() {
      this.editProjectModalOpen = false
    },
    async saveProjectUpdate() {
      if (!this.editProjectData.name.trim()) {
        alert('请输入项目名称')
        return
      }
      await this.withOperationLoading('正在保存项目修改...', async () => {
        try {
          // 使用项目API模块更新项目
          const { projectAPI } = await import('@/api/project')
          console.log('使用项目API模块更新项目...')
          console.log('项目ID:', this.project.id, '类型:', typeof this.project.id)
          console.log('更新数据:', this.editProjectData)
          const response = await projectAPI.updateProject(this.project.id, this.editProjectData)
          console.log('更新项目API返回结果:', response)
          console.log('更新后的项目数据:', response.data)
          // 检查API返回结果
          if (response.code === 200) {
            // 使用后端返回的最新数据更新本地项目
            if (response.data) {
              // 更新项目ID（确保使用后端返回的ID）
              this.project.id = response.data.id
              this.project.name = response.data.name
              this.project.title = response.data.name
              this.project.description = response.data.description
              this.project.startDate = response.data.startDate
              this.project.endDate = response.data.endDate
              this.project.visibility = response.data.visibility
              this.project.status = response.data.status
              this.project.imageUrl = response.data.imageUrl
              this.project.image = response.data.imageUrl
              // 更新项目周期显示
              if (response.data.startDate && response.data.endDate) {
                this.project.period = `${response.data.startDate} 至 ${response.data.endDate}`
              }
              console.log('项目更新成功，最新项目ID:', this.project.id)
              console.log('更新后的项目完整数据:', this.project)
            } else {
              // 如果后端没有返回数据，使用编辑表单的数据
              this.project.name = this.editProjectData.name
              this.project.title = this.editProjectData.name
              this.project.description = this.editProjectData.description
              this.project.startDate = this.editProjectData.startDate
              this.project.endDate = this.editProjectData.endDate
              this.project.visibility = this.editProjectData.visibility
              this.project.status = this.editProjectData.status
              // 更新项目周期显示
              if (this.editProjectData.startDate && this.editProjectData.endDate) {
                this.project.period = `${this.editProjectData.startDate} 至 ${this.editProjectData.endDate}`
              }
            }
            // 保存到localStorage
            this.saveProjectData()
            this.closeEditProjectModal()
            this.showSuccessToast('项目更新成功！')
          } else {
            alert('更新失败：' + (response.msg || '未知错误'))
          }
        } catch (error) {
          console.error('更新项目失败:', error)
          alert('更新项目失败，请稍后重试')
        }
      })
    },
    async changeStatus(newStatus) {
      // 更改项目状态
      if (newStatus && newStatus !== this.project.status) {
        try {
          // 使用项目API模块更新项目状态
          const { projectAPI } = await import('@/api/project')
          console.log('使用项目API模块更新项目状态...')
          const response = await projectAPI.updateProjectStatus(this.project.id, newStatus)
          console.log('更新项目状态API返回结果:', response)
          // 检查API返回结果
          if (response.code === 200) {
            this.project.status = newStatus
            this.saveProjectData()
            this.statusDropdownOpen = false
            this.showSuccessToast('项目状态已更新！')
            console.log('项目状态已更改为:', newStatus)
          } else {
            alert('状态更新失败：' + (response.msg || '未知错误'))
          }
        } catch (error) {
          console.error('更新项目状态失败:', error)
          alert('更新项目状态失败，请稍后重试')
        }
      }
    },
    // 打开删除项目确认弹窗
    deleteProject() {
      this.deleteProjectConfirmOpen = true
    },
    // 取消删除项目
    cancelDeleteProject() {
      this.deleteProjectConfirmOpen = false
    },
    // 确认删除项目（在居中弹窗中点击“确认删除”）
    async confirmDeleteProject() {
      try {
        const { projectAPI } = await import('@/api/project')
        console.log('====== 开始删除项目 ======')
        console.log('项目ID:', this.project.id, '类型:', typeof this.project.id)
        console.log('项目名称:', this.project.name || this.project.title)
        const response = await projectAPI.deleteProject(this.project.id)
        console.log('删除项目API返回结果:', response)
        console.log('返回code:', response?.code)
        console.log('返回msg:', response?.msg)
        if (response && response.code === 200) {
          const savedProjects = JSON.parse(localStorage.getItem('projects') || '[]')
          console.log('删除前的项目列表:', savedProjects.map(p => ({ id: p.id, name: p.name || p.title })))
          const updatedProjects = savedProjects.filter(p => String(p.id) !== String(this.project.id))
          console.log('删除后的项目列表:', updatedProjects.map(p => ({ id: p.id, name: p.name || p.title })))
          localStorage.setItem('projects', JSON.stringify(updatedProjects))
          this.showSuccessToast('项目删除成功！')
          console.log('====== 项目删除完成，即将跳转 ======')
          setTimeout(() => {
            this.$router.push('/project-square')
          }, 1500)
        } else {
          const errorMsg = response?.msg || '未知错误'
          console.error('删除失败，错误信息:', errorMsg)
          this.showSuccessToast('删除失败：' + errorMsg)
        }
      } catch (error) {
        console.error('删除项目失败:', error)
        alert('删除项目失败，请稍后重试')
      }
    },
    // 验证新建任务截止日期
    validateNewTaskDueDate() {
      this.newTask.dateError = ''
      if (this.newTask.dueDate && new Date(this.newTask.dueDate) < new Date(this.today)) {
        this.newTask.dateError = '任务截止日期不能早于今天'
        return false
      }
      return true
    },
    // 任务操作功能
    createTask() {
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能新建任务')
        return
      }
      this.taskModalOpen = true
      // 重置表单
      this.newTask = {
        title: '',
        description: '',
        dueDate: '',
        priority: '中',
        status: '待接取',
        dateError: '',
        participantCount: null,
        isMilestone: false
      }
    },
    closeTaskModal() {
      this.taskModalOpen = false
    },
    async saveNewTask() {
      // 防止重复点击
      if (this.isCreatingTask) {
        console.log('[saveNewTask] 任务正在创建中，忽略重复点击')
        return
      }
      if (!this.newTask.title.trim()) {
        alert('请输入任务标题')
        return
      }
      // 验证截止日期不能早于今天
      if (this.newTask.dueDate && new Date(this.newTask.dueDate) < new Date(this.today)) {
        alert('任务截止日期不能早于今天')
        return
      }
      this.isCreatingTask = true
      await this.withOperationLoading('正在创建任务...', async () => {
        try {
          const { taskAPI } = await import('@/api/task')
          const taskData = {
            projectId: this.project.id,
            title: this.newTask.title.trim(),
            description: this.newTask.description.trim(),
            priority: this.getPriorityValue(this.newTask.priority),
            dueDate: this.newTask.dueDate || null,
            assigneeIds: [],
            requiredPeople: this.newTask.participantCount || 1,
            isMilestone: this.newTask.isMilestone || false
          }
          console.log('[saveNewTask] 创建任务，数据:', taskData)
          const response = await taskAPI.createTask(taskData)
          console.log('[saveNewTask] API返回结果:', response)
          if (response && response.code === 200) {
            await this.loadProjectTasks()
            this.closeTaskModal()
            this.showSuccessToast('任务创建成功！')
          } else {
            if (response && response.code === 401) {
              alert('登录已过期，请重新登录')
              localStorage.removeItem('access_token')
              localStorage.removeItem('refresh_token')
              localStorage.removeItem('user_info')
              window.location.href = '/login'
            } else {
              alert('创建任务失败：' + (response.msg || '未知错误'))
            }
          }
        } catch (error) {
          console.error('[saveNewTask] 创建任务失败:', error)
          if (error && (error.code === 401 || error.status === 401 || 
              (error.response && error.response.status === 401))) {
            alert('登录已过期，请重新登录')
            localStorage.removeItem('access_token')
            localStorage.removeItem('refresh_token')
            localStorage.removeItem('user_info')
            window.location.href = '/login'
          } else {
            const errorMsg = error?.msg || error?.message || String(error)
            if (errorMsg.includes('登录') || errorMsg.includes('Token') || errorMsg.includes('认证')) {
              alert('登录已过期，请重新登录')
              localStorage.removeItem('access_token')
              localStorage.removeItem('refresh_token')
              localStorage.removeItem('user_info')
              window.location.href = '/login'
            } else {
              alert('创建任务失败：' + errorMsg)
            }
          }
        } finally {
          setTimeout(() => {
            this.isCreatingTask = false
          }, 1000)
        }
      })
    },
    editTask(task) {
      // 归档项目：不允许编辑任务（按钮已禁用，这里再兜底一次）
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能编辑任务')
        return
      }
      // 设置编辑数据
      this.editTaskData = {
        title: task.title,
        description: task.description || '',
        dueDate: task.date || task.dueDate || '',
        priority: task.priority,
        taskId: task.id
      }
      // 打开编辑弹窗
      this.editTaskModalOpen = true
    },
    closeEditTaskModal() {
      this.editTaskModalOpen = false
      this.editTaskData = {
        title: '',
        description: '',
        dueDate: '',
        priority: '中',
        taskId: null
      }
    },
    async saveEditTask() {
      // 归档项目：不允许保存任务修改
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能修改任务')
        this.closeEditTaskModal()
        return
      }
      if (!this.editTaskData.title.trim()) {
        alert('请输入任务标题')
        return
      }
      // 验证截止日期不能早于今天
      if (this.editTaskData.dueDate && new Date(this.editTaskData.dueDate) < new Date(this.today)) {
        alert('任务截止日期不能早于今天')
        return
      }
      await this.withOperationLoading('正在保存任务修改...', async () => {
        try {
          const { taskAPI } = await import('@/api/task')
          const updateData = {
            title: this.editTaskData.title.trim(),
            description: this.editTaskData.description.trim(),
            priority: this.getPriorityValue(this.editTaskData.priority),
            dueDate: this.editTaskData.dueDate || null
          }
          console.log('[saveEditTask] 更新任务，ID:', this.editTaskData.taskId, '数据:', updateData)
          const response = await taskAPI.updateTask(this.editTaskData.taskId, updateData)
          console.log('[saveEditTask] API返回结果:', response)
          if (response && response.code === 200) {
            await this.loadProjectTasks()
            this.closeEditTaskModal()
            this.showSuccessToast('任务更新成功！')
          } else {
            alert('更新任务失败：' + (response.msg || '未知错误'))
          }
        } catch (error) {
          console.error('[saveEditTask] 更新任务失败:', error)
          alert('更新任务失败，请稍后重试')
        }
      })
    },
    validateEditTaskDueDate() {
      this.editTaskData.dateError = ''
      if (this.editTaskData.dueDate && new Date(this.editTaskData.dueDate) < new Date(this.today)) {
        this.editTaskData.dateError = '任务截止日期不能早于今天'
        return false
      }
      return true
    },
    // 打开删除任务确认弹窗
    deleteTask(taskId) {
      // 归档项目：不允许删除任务（双重保护，按钮已禁用，这里再兜底一次）
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能删除任务')
        return
      }
      this.taskToDelete = taskId
      this.deleteTaskConfirmOpen = true
    },
    // 取消删除任务
    cancelDeleteTask() {
      this.deleteTaskConfirmOpen = false
      this.taskToDelete = null
    },
    // 确认删除任务（在居中弹窗中点击"确定"）
    async confirmDeleteTask() {
      // 再次校验项目状态，防止在归档后旧弹窗仍然可用
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能删除任务')
        this.cancelDeleteTask()
        return
      }
      if (!this.taskToDelete) {
        this.cancelDeleteTask()
        return
      }
      try {
        // 导入任务API
        const { taskAPI } = await import('@/api/task')
        console.log('[deleteTask] 删除任务，任务ID:', this.taskToDelete)
        // 调用后端API删除任务
        const response = await taskAPI.deleteTask(this.taskToDelete)
        console.log('[deleteTask] API返回结果:', response)
        if (response && response.code === 200) {
          console.log('[deleteTask] ✅ 任务删除成功，重新从后端加载任务列表')
          // ✅ 重新从后端加载最新的任务列表，确保数据一致性
          await this.loadProjectTasks()
          this.showSuccessToast('任务已删除！')
          this.cancelDeleteTask()
        } else {
          this.showErrorDialog('删除任务失败：' + (response.msg || '未知错误'))
          this.cancelDeleteTask()
        }
      } catch (error) {
        console.error('[deleteTask] 删除任务失败:', error)
        this.showErrorDialog('删除任务失败，请稍后重试')
        this.cancelDeleteTask()
      }
    },
    // 显示错误提示弹窗
    showErrorDialog(message) {
      this.errorMessage = message
      this.errorDialogOpen = true
    },
    // 关闭错误提示弹窗
    closeErrorDialog() {
      this.errorDialogOpen = false
      this.errorMessage = ''
    },
    handleClickOutside(event) {
      if (!event.target.closest('.dropdown')) {
        this.taskTypeOpen = false
        this.statusDropdownOpen = false
      }
      // 关闭所有成员角色下拉菜单
      if (!event.target.closest('.member-role-dropdown')) {
        Object.keys(this.memberRoleDropdownOpen).forEach(id => {
          if (this.memberRoleDropdownOpen[id]) {
            this.$set(this.memberRoleDropdownOpen, id, false)
          }
        })
      }
    },
    getCurrentUserId() {
      // 从localStorage获取当前用户ID
      const savedUserInfo = localStorage.getItem('user_info')
      if (savedUserInfo) {
        try {
          const userInfo = JSON.parse(savedUserInfo)
          const userId = userInfo.id || userInfo.userId || null
          // 统一转换为字符串，确保类型一致
          if (userId != null) {
            return String(userId)
          }
          return null
        } catch (error) {
          console.error('解析用户信息失败:', error)
          return null
        }
      }
      return null
    },
    /**
     * 检查当前用户是否是任务的接取者
     * @param {Object} task - 任务对象
     * @returns {Boolean} 当前用户是否是接取者
     */
    isCurrentUserAssignee(task) {
      if (!task || !task.assignee_id || !Array.isArray(task.assignee_id) || task.assignee_id.length === 0) {
        return false
      }
      const currentUserId = this.getCurrentUserId()
      if (!currentUserId) {
        return false
      }
      // 将当前用户ID转换为字符串进行比较（因为assignee_id可能是字符串数组）
      const currentUserIdStr = String(currentUserId)
      // 检查assignee_id数组中是否包含当前用户ID
      return task.assignee_id.some(id => String(id) === currentUserIdStr)
    },
    /**
     * 检查当前用户是否可以接取任务
     * @param {Object} task - 任务对象
     * @returns {Boolean} 是否可以接取
     */
    canClaimTask(task) {
      console.log('[canClaimTask] 检查任务:', task)
      if (!task) {
        console.log('[canClaimTask] 任务不存在')
        return false
      }
      // 项目已归档：不允许接取任务
      if (this.isArchived) {
        console.log('[canClaimTask] 项目已归档，禁止接取任务')
        return false
      }
      // 如果任务已完成，不能接取
      if (task.status === '完成' || task.status === 'DONE' || task.status_value === 'DONE') {
        console.log('[canClaimTask] 任务已完成')
        return false
      }
      // 如果当前用户已经接取，不能重复接取
      if (this.isCurrentUserAssignee(task)) {
        console.log('[canClaimTask] 当前用户已接取')
        return false
      }
      // 如果任务已满员，不能接取
      if (this.isTaskFull(task)) {
        console.log('[canClaimTask] 任务已满员')
        return false
      }
      // 能看到项目详情页面的用户都是项目成员，所以不需要额外检查
      console.log('[canClaimTask] 可以接取任务')
      return true
    },
    /**
     * 检查任务是否已满员
     * @param {Object} task - 任务对象
     * @returns {Boolean} 是否已满员
     */
    isTaskFull(task) {
      if (!task || !task.participantCount) {
        return false // 没有设置人数限制，不会满员
      }
      const currentCount = task.assignees ? task.assignees.length : 0
      return currentCount >= task.participantCount
    },
    getCurrentUserName() {
      // 从localStorage获取当前用户姓名
      const savedUserInfo = localStorage.getItem('user_info')
      if (savedUserInfo) {
        try {
          const userInfo = JSON.parse(savedUserInfo)
          return userInfo.name || userInfo.nickname || '用户'
        } catch (error) {
          console.error('解析用户信息失败:', error)
          return '用户'
        }
      }
      return '用户'
    },
    getPriorityDisplay(priority) {
      // 将数据库的英文优先级转换为中文显示
      const priorityMap = {
        'HIGH': '高',
        'MEDIUM': '中',
        'LOW': '低'
      }
      return priorityMap[priority] || '中'
    },
    getPriorityValue(priority) {
      // 将中文优先级转换为数据库的英文值
      const valueMap = {
        '高': 'HIGH',
        '中': 'MEDIUM',
        '低': 'LOW'
      }
      return valueMap[priority] || 'MEDIUM'
    },
    getUserNameById(userId) {
      // 根据用户ID获取用户姓名
      if (userId === 1) {
        return this.getCurrentUserName()
      }
      // 这里可以从用户服务或本地存储获取用户信息
      // 暂时返回默认值
      return '用户' + userId
    },
    getStatusDisplay(status) {
      // 将数据库的英文状态转换为中文显示
      const statusMap = {
        // 项目状态
        'PLANNING': '规划中',
        'ONGOING': '进行中',
        'COMPLETED': '已完成',
        'ARCHIVED': '已归档',
        // 任务状态（后端枚举：TODO, IN_PROGRESS, BLOCKED, PENDING_REVIEW, DONE）
        'TODO': '待接取',
        'IN_PROGRESS': '进行中',
        'BLOCKED': '阻塞',
        'PENDING_REVIEW': '待审核',
        'DONE': '完成',
        // 兼容旧数据
        'PENDING': '待接取',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status || '未知'
    },
    getStatusValue(status) {
      // 将中文状态转换为数据库的英文枚举值
      const reverseMap = {
        // 项目状态映射
        '规划中': 'PLANNING',
        '进行中': 'IN_PROGRESS',
        '已完成': 'COMPLETED',
        '已归档': 'ARCHIVED',
        // 任务状态映射（后端枚举：TODO, IN_PROGRESS, BLOCKED, PENDING_REVIEW, DONE）
        '待接取': 'TODO',
        '阻塞': 'BLOCKED',
        '待审核': 'PENDING_REVIEW',
        '完成': 'DONE',
        '已取消': 'CANCELLED'
      }
      // ✅ 先检查是否在映射表中
      if (reverseMap[status]) {
        return reverseMap[status]
      }
      // ✅ 如果已经是大写枚举值（如 TODO, IN_PROGRESS），直接返回
      const validEnums = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'PENDING_REVIEW', 'DONE', 'PLANNING', 'ONGOING', 'COMPLETED', 'ARCHIVED', 'CANCELLED']
      if (status && validEnums.includes(status.toUpperCase())) {
        return status.toUpperCase()
      }
      // ✅ 默认返回TODO
      return 'TODO'
    },
    toggleTaskStatusDropdown(task) {
      // 如果任务状态为"待接取"且没有执行者，不允许修改状态
      if (task.status === '待接取' && (!task.assignee_name || task.assignee_name === '')) {
        this.showSuccessToast('任务未被接取，无法修改状态。请先接取任务。')
        return
      }
      // 关闭其他任务的状态菜单
      this.tasks.forEach(t => {
        if (t.id !== task.id) {
          this.$set(t, 'showStatusMenu', false)
        }
      })
      // 切换当前任务的状态菜单
      this.$set(task, 'showStatusMenu', !task.showStatusMenu)
    },
    async changeTaskStatus(task, newStatus) {
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能更改任务状态')
        return
      }
      await this.withOperationLoading('正在更新任务状态...', async () => {
        try {
          const { taskAPI } = await import('@/api/task')
          const statusValue = this.getStatusValue(newStatus)
          console.log(`[changeTaskStatus] 更新任务状态，任务ID: ${task.id}, 新状态: ${newStatus} (${statusValue})`)
          const response = await taskAPI.updateTaskStatus(task.id, statusValue)
          console.log('[changeTaskStatus] API返回结果:', response)
          if (response && response.code === 200) {
            console.log('[changeTaskStatus] ✅ 任务状态更新成功，重新加载任务列表')
            this.$set(task, 'showStatusMenu', false)
            await this.loadProjectTasks()
            this.showSuccessToast('任务状态已更新！')
            console.log(`任务"${task.title}"状态已更改为: ${newStatus} (${statusValue})`)
            this.$root.$emit('taskStatusChanged', {
              projectId: this.project.id,
              taskId: task.id,
              newStatus: newStatus,
              statusValue: statusValue
            })
          } else {
            alert('更新任务状态失败：' + (response.msg || '未知错误'))
            this.$set(task, 'showStatusMenu', false)
          }
        } catch (error) {
          console.error('[changeTaskStatus] 更新任务状态失败:', error)
          alert('更新任务状态失败，请稍后重试')
          this.$set(task, 'showStatusMenu', false)
        }
      })
    },
    // 打开接取任务确认弹窗
    assignTask(task) {
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能接取任务')
        return
      }
      this.taskToClaim = task
      this.claimTaskConfirmOpen = true
    },
    // 取消接取任务
    cancelClaimTask() {
      this.claimTaskConfirmOpen = false
      this.taskToClaim = null
    },
    // 确认接取任务（在居中弹窗中点击“确认接取”）
    async confirmClaimTask() {
      const task = this.taskToClaim
      if (!task) {
        this.cancelClaimTask()
        return
      }
      await this.withOperationLoading('正在接取任务...', async () => {
        try {
          const currentUserId = this.getCurrentUserId()
          const currentUserName = this.getCurrentUserName()
          console.log('[assignTask] 开始接取任务, ID:', task.id, '当前状态:', task.status)
          const { taskAPI } = await import('@/api/task')
          const response = await taskAPI.claimTask(task.id)
          console.log('[assignTask] 后端返回:', response)
          if (response && response.code === 200) {
            console.log('[assignTask] ✅ 任务接取成功')
            await new Promise(resolve => setTimeout(resolve, 300))
            await this.loadProjectTasks()
            this.$nextTick(() => {
              this.$forceUpdate()
            })
            this.showSuccessToast(`成功接取任务: ${task.title}`)
            const updatedTask = this.tasks.find(t => t.id === task.id)
            console.log('[assignTask] 更新后的任务状态:', updatedTask?.status, '执行者:', updatedTask?.assignee_name)
            if (updatedTask && updatedTask.status === '待接取') {
              console.warn('[assignTask] ⚠️ 状态未更新，再次尝试加载')
              await new Promise(resolve => setTimeout(resolve, 500))
              await this.loadProjectTasks()
              this.$forceUpdate()
            }
          } else {
            alert('接取任务失败：' + (response.msg || '未知错误'))
          }
        } catch (error) {
          console.error('[assignTask] 接取任务失败:', error)
          alert('接取任务失败，请稍后重试')
        } finally {
          this.cancelClaimTask()
        }
      })
    },
    // 打开分配任务模态框
    openAssignTaskModal(task) {
      // 归档项目：不允许分配任务
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能分配任务')
        return
      }
      this.taskToAssign = task
      this.selectedAssigneeId = null
      this.assignTaskModalOpen = true
    },
    // 关闭分配任务模态框
    closeAssignTaskModal() {
      this.assignTaskModalOpen = false
      this.taskToAssign = null
      this.selectedAssigneeId = null
    },
    // 确认分配任务
    async confirmAssignTask() {
      // 再次校验项目状态，避免归档后残留弹窗还能提交
      if (this.isArchived) {
        this.showErrorToast('项目已归档，只能查看，不能分配任务')
        this.closeAssignTaskModal()
        return
      }
      if (!this.selectedAssigneeId || !this.taskToAssign) return
      const selectedMember = this.teamMembers.find(m => m.id === this.selectedAssigneeId)
      if (!selectedMember) {
        alert('未找到选中的成员')
        return
      }
      
      // 检查该成员是否已经是执行者
      const currentAssigneeIds = this.taskToAssign.assignee_id || []
      if (currentAssigneeIds.includes(String(this.selectedAssigneeId))) {
        alert('该成员已经是任务执行者')
        return
      }
      
      // 检查任务是否已满员
      if (this.isTaskFull(this.taskToAssign)) {
        alert(`任务已达到最大人数限制(${this.taskToAssign.participantCount})，无法再分配`)
        return
      }
      
      await this.withOperationLoading('正在分配任务...', async () => {
        try {
          console.log('[confirmAssignTask] 开始分配任务, ID:', this.taskToAssign.id, '当前状态:', this.taskToAssign.status)
          console.log('[confirmAssignTask] 当前执行者:', currentAssigneeIds)
          console.log('[confirmAssignTask] 新增执行者:', this.selectedAssigneeId)
          const updatedAssigneeIds = [...currentAssigneeIds.map(id => String(id)), String(this.selectedAssigneeId)]
          console.log('[confirmAssignTask] 更新后的执行者列表:', updatedAssigneeIds)
          const { taskAPI } = await import('@/api/task')
          const response = await taskAPI.assignTask(this.taskToAssign.id, updatedAssigneeIds)
          console.log('[confirmAssignTask] 后端返回:', response)
          if (response && response.code === 200) {
            console.log('[confirmAssignTask] ✅ 任务分配成功')
            await new Promise(resolve => setTimeout(resolve, 300))
            await this.loadProjectTasks()
            this.$nextTick(() => {
              this.$forceUpdate()
            })
            this.showSuccessToast(`成功将任务"${this.taskToAssign.title}"分配给 ${selectedMember.name}`)
            const assignedTaskId = this.taskToAssign.id
            this.closeAssignTaskModal()
            const updatedTask = this.tasks.find(t => t.id === assignedTaskId)
            console.log('[confirmAssignTask] 更新后的任务状态:', updatedTask?.status, '执行者:', updatedTask?.assignee_name)
            if (updatedTask && updatedTask.status === '待接取') {
              console.warn('[confirmAssignTask] ⚠️ 状态未更新，再次尝试加载')
              await new Promise(resolve => setTimeout(resolve, 500))
              await this.loadProjectTasks()
              this.$forceUpdate()
            }
          } else {
            alert('分配任务失败：' + (response.msg || '未知错误'))
          }
        } catch (error) {
          console.error('[confirmAssignTask] 分配任务失败:', error)
          alert('分配任务失败，请稍后重试')
        }
      })
    },
    // ==================== 任务提交相关方法 (新) ====================
    /**
     * 打开任务提交弹窗
     */
    async openTaskSubmissionModal(task) {
      // 检查是否为任务执行者
      const currentUserId = this.getCurrentUserId()
      const isAssignee = this.isTaskAssignee(task, currentUserId)
      // 调试日志
      console.log('[openTaskSubmissionModal] 检查任务执行者:', {
        taskId: task.id,
        taskTitle: task.title,
        currentUserId: currentUserId,
        assignee_id: task.assignee_id,
        assignee_ids: task.assignee_ids,
        assigneeIds: task.assigneeIds,
        isAssignee: isAssignee
      })
      if (!isAssignee) {
        console.warn('[openTaskSubmissionModal] 权限检查失败：当前用户不是任务执行者')
        this.showErrorToast('只有任务执行者才能提交任务')
        return
      }
      // 检查任务状态
      if (task.status === 'DONE' || task.status === '已完成') {
        this.showErrorToast('任务已完成，无需重复提交')
        return
      }
      console.log('[openTaskSubmissionModal] 权限检查通过，打开提交弹窗')
      this.taskToSubmit = task
      // 获取最新提交（用于编辑模式）
      try {
        const response = await getLatestSubmission(task.id)
        if (response.code === 200 && response.data) {
          // 检查是否是当前用户的提交
          const currentUserId = this.getCurrentUserId()
          const submitterId = response.data.submitterId || response.data.submitter?.id
          if (String(submitterId) === String(currentUserId)) {
            this.latestSubmissionForEdit = response.data
            console.log('[openTaskSubmissionModal] 找到之前的提交，进入编辑模式:', this.latestSubmissionForEdit)
          } else {
            this.latestSubmissionForEdit = null
          }
        } else {
          this.latestSubmissionForEdit = null
        }
      } catch (error) {
        console.error('[openTaskSubmissionModal] 获取最新提交失败:', error)
        this.latestSubmissionForEdit = null
      }
      this.taskSubmissionModalVisible = true
      // 加载任务提交历史
      this.loadTaskSubmissions(task.id)
    },
    /**
     * 关闭任务提交弹窗
     */
    closeTaskSubmissionModal() {
      this.taskSubmissionModalVisible = false
      this.taskToSubmit = null
      this.latestSubmissionForEdit = null
    },
    /**
     * 任务提交成功回调
     */
    async handleTaskSubmitSuccess(submission) {
      console.log('任务提交成功:', submission)
      this.showSuccessToast('任务提交成功，等待审核')
      // 立即更新当前任务的hasSubmission标志
      if (this.taskToSubmit && this.taskToSubmit.id) {
        const taskId = this.taskToSubmit.id
        // 更新tasks数组中的任务
        const taskIndex = this.tasks.findIndex(t => t.id === taskId)
        if (taskIndex !== -1) {
          this.$set(this.tasks[taskIndex], 'hasSubmission', true)
          this.$set(this.tasks[taskIndex], 'status', '待审核')
        }
      }
      // 关闭弹窗
      this.closeTaskSubmissionModal()
      // 刷新任务列表（后台同步）
      await this.withOperationLoading('正在刷新任务列表...', async () => {
        await this.loadProjectTasks()
      })
    },
    /**
     * 加载任务提交历史
     */
    async loadTaskSubmissions(taskId) {
      try {
        const response = await getTaskSubmissions(taskId)
        if (response.code === 200) {
          this.taskSubmissions = response.data || []
          console.log('任务提交历史:', this.taskSubmissions)
        }
      } catch (error) {
        console.error('加载任务提交历史失败', error)
      }
    },
    /**
     * 检查当前用户是否为任务执行者
     */
    isTaskAssignee(task, userId) {
      if (!task || !userId) return false
      const userIdStr = String(userId)
      // 检查 assignee_id 数组（实际使用的字段）
      if (task.assignee_id && Array.isArray(task.assignee_id)) {
        return task.assignee_id.some(id => String(id) === userIdStr)
      }
      // 检查 assignee_ids 数组（备用字段名）
      if (task.assignee_ids && Array.isArray(task.assignee_ids)) {
        return task.assignee_ids.includes(userId) || 
               task.assignee_ids.includes(userIdStr)
      }
      // 检查 assigneeIds 数组（备用字段名）
      if (task.assigneeIds && Array.isArray(task.assigneeIds)) {
        return task.assigneeIds.includes(userId) || 
               task.assigneeIds.includes(userIdStr)
      }
      return false
    },
    /**
     * 从任务详情弹窗触发提交
     */
    handleSubmitTask(task) {
      // 关闭任务详情弹窗
      this.closeTaskDetailModal()
      // 打开任务提交弹窗
      this.openTaskSubmissionModal(task)
    },
    // ==================== 任务审核相关方法 (新) ====================
    /**
     * 打开任务审核弹窗（仅项目负责人可用）
     */
    async openTaskReviewModal(task) {
      // 检查是否有审核权限（仅项目负责人）
      if (!this.isProjectManager) {
        this.showErrorToast('只有项目负责人才能审核提交')
        return
      }
      try {
        // 获取任务的最新提交
        const response = await getLatestSubmission(task.id)
        let submission = null
        if (response.code === 200 && response.data) {
          submission = response.data
        }
        // 无论是否有提交，都打开审核弹窗
        // 如果没有提交，submission 为 null，弹窗会显示"暂无提交"
        this.submissionToReview = submission
        this.taskReviewModalVisible = true
      } catch (error) {
        console.error('获取待审核提交失败', error)
        // 即使获取失败，也打开弹窗，显示"暂无提交"
        this.submissionToReview = null
        this.taskReviewModalVisible = true
      }
    },
    /**
     * 关闭审核弹窗
     */
    closeTaskReviewModal() {
      this.taskReviewModalVisible = false
      this.submissionToReview = null
    },
    /**
     * 审核成功回调
     */
    async handleReviewSuccess(submission) {
      console.log('[handleReviewSuccess] 审核完成，接收到的提交数据:', submission)
      if (!submission) {
        console.warn('[handleReviewSuccess] ⚠️ 提交数据为空')
        this.showSuccessToast('审核完成')
        await this.loadProjectTasks()
        this.closeTaskReviewModal()
        return
      }
      const reviewStatus = submission.reviewStatus
      const taskId = submission.taskId
      console.log('[handleReviewSuccess] 审核状态:', reviewStatus, '任务ID:', taskId)
      if (!taskId) {
        console.error('[handleReviewSuccess] ❌ 任务ID为空，无法更新任务状态')
        this.showSuccessToast('审核完成，但无法更新任务状态（缺少任务ID）')
        await this.loadProjectTasks()
        this.closeTaskReviewModal()
        return
      }
      // 根据审核结果更新任务状态
      if (reviewStatus === 'APPROVED') {
        // 审核通过：更新任务状态为"完成"
        try {
          const { taskAPI } = await import('@/api/task')
          const response = await taskAPI.updateTaskStatus(taskId, 'DONE')
          if (response.code === 200) {
            this.showSuccessToast('审核通过，任务已完成')
            console.log('[handleReviewSuccess] ✅ 任务状态已更新为完成')
            // 等待状态更新完成后再刷新任务列表
            await new Promise(resolve => setTimeout(resolve, 300))
            // 刷新任务列表
            await this.loadProjectTasks()
            
            // ✅ 触发全局事件，通知首页刷新任务列表
            this.$root.$emit('taskStatusChanged', {
              projectId: this.project.id,
              taskId: taskId,
              newStatus: '完成',
              statusValue: 'DONE',
              reviewStatus: 'APPROVED'
            })
          } else {
            console.warn('[handleReviewSuccess] 更新任务状态失败:', response.msg)
            this.showSuccessToast('审核通过')
            // 即使更新失败也刷新任务列表
            await this.loadProjectTasks()
            // 触发事件
            this.$root.$emit('taskStatusChanged', {
              projectId: this.project.id,
              taskId: taskId,
              reviewStatus: 'APPROVED'
            })
          }
        } catch (error) {
          console.error('[handleReviewSuccess] 更新任务状态失败:', error)
          this.showSuccessToast('审核通过')
          // 即使更新失败也刷新任务列表
          await this.loadProjectTasks()
          // 触发事件
          this.$root.$emit('taskStatusChanged', {
            projectId: this.project.id,
            taskId: taskId,
            reviewStatus: 'APPROVED'
          })
        }
      } else if (reviewStatus === 'REJECTED') {
        // 审核拒绝：更新任务状态为"进行中"
        console.log('[handleReviewSuccess] 开始更新任务状态为进行中，任务ID:', taskId)
        try {
          const { taskAPI } = await import('@/api/task')
          const response = await taskAPI.updateTaskStatus(taskId, 'IN_PROGRESS')
          console.log('[handleReviewSuccess] 状态更新API返回:', response)
          if (response && response.code === 200) {
            this.showSuccessToast('审核拒绝，任务状态已更新为进行中')
            console.log('[handleReviewSuccess] ✅ 任务状态已更新为进行中')
            // 等待状态更新完成后再刷新任务列表
            await new Promise(resolve => setTimeout(resolve, 500))
            // 刷新任务列表
            await this.loadProjectTasks()
            console.log('[handleReviewSuccess] ✅ 任务列表已刷新')
            
            // ✅ 触发全局事件，通知首页刷新任务列表（包括被打回的任务）
            this.$root.$emit('taskStatusChanged', {
              projectId: this.project.id,
              taskId: taskId,
              newStatus: '进行中',
              statusValue: 'IN_PROGRESS',
              reviewStatus: 'REJECTED' // 标记这是审核拒绝
            })
            console.log('[handleReviewSuccess] ✅ 已触发 taskStatusChanged 事件，通知首页刷新')
          } else {
            console.error('[handleReviewSuccess] ❌ 更新任务状态失败，响应:', response)
            console.error('[handleReviewSuccess] 错误信息:', response?.msg || '未知错误')
            this.showSuccessToast('审核拒绝，但状态更新失败: ' + (response?.msg || '未知错误'))
            // 即使更新失败也刷新任务列表
            await this.loadProjectTasks()
            // 即使状态更新失败，也触发事件（因为提交记录已经被标记为REJECTED）
            this.$root.$emit('taskStatusChanged', {
              projectId: this.project.id,
              taskId: taskId,
              reviewStatus: 'REJECTED'
            })
          }
        } catch (error) {
          console.error('[handleReviewSuccess] ❌ 更新任务状态异常:', error)
          console.error('[handleReviewSuccess] 错误详情:', error.message, error.stack)
          this.showSuccessToast('审核拒绝，但状态更新异常: ' + (error.message || '未知错误'))
          // 即使更新失败也刷新任务列表
          await this.loadProjectTasks()
          // 即使状态更新异常，也触发事件（因为提交记录已经被标记为REJECTED）
          this.$root.$emit('taskStatusChanged', {
            projectId: this.project.id,
            taskId: taskId,
            reviewStatus: 'REJECTED'
          })
        }
      } else {
        this.showSuccessToast('审核完成')
        // 刷新任务列表
        await this.loadProjectTasks()
      }
      // 关闭弹窗
      this.closeTaskReviewModal()
    },
    /**
     * 查看任务提交历史
     */
    async viewTaskSubmissions(task) {
      try {
        const response = await getTaskSubmissions(task.id)
        if (response.code === 200) {
          const submissions = response.data || []
          if (submissions.length === 0) {
            this.showErrorToast('该任务暂无提交记录')
            return
          }
          // 显示提交历史
          console.log('提交历史:', submissions)
          this.showSuccessToast(`该任务共有 ${submissions.length} 条提交记录`)
          // TODO: 可以创建一个提交历史查看弹窗
        }
      } catch (error) {
        console.error('获取提交历史失败', error)
        this.showErrorToast('获取提交历史失败')
      }
    },
    // ==================== 辅助方法 ====================
    /**
     * 显示错误提示
     */
    showErrorToast(message) {
      alert(message) // 简单实现，可以替换为更好的Toast组件
    },
    statusClass(status) {
      // 处理项目状态类名
      if (status === 'PLANNING' || status === '规划中') return 'planning'
      if (status === 'IN_PROGRESS' || status === '进行中') return 'ongoing'
      if (status === 'PAUSED' || status === '暂停') return 'paused'
      if (status === 'COMPLETED' || status === '完成') return 'completed'
      if (status === 'CANCELLED' || status === '已取消') return 'cancelled'
      return 'ongoing'
    },
    priorityClass(priority) {
      if (priority === '高') return '高'
      if (priority === '中') return '中'
      return '低'
    },
    isTaskOverdue(task) {
      // 判断任务是否已逾期
      if (!task) return false
      const dueDate = task.date || task.dueDate || task.due_date
      if (!dueDate) return false
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const deadline = new Date(dueDate)
      deadline.setHours(0, 0, 0, 0)
      // 只有截止日期在今天之前（不包括今天）才算逾期
      return deadline < today
    },
    isTaskNearDeadline(task) {
      // 判断任务是否临近截止（3天内）
      if (!task) return false
      const dueDate = task.date || task.dueDate || task.due_date
      if (!dueDate) return false
      const now = new Date()
      const deadline = new Date(dueDate)
      const diffTime = deadline - now
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      // 0-3天内截止且未逾期返回true
      return diffDays >= 0 && diffDays <= 3 && !this.isTaskOverdue(task)
    },
    toggleTaskTypeDropdown() {
      this.taskTypeOpen = !this.taskTypeOpen
    },
    toggleStatusDropdown() {
      this.statusDropdownOpen = !this.statusDropdownOpen
    },
    selectTaskType(type) {
      this.selectedTaskType = type
      this.taskTypeOpen = false
    },
    triggerImageUpload() {
      // 触发文件上传输入
      this.$refs.projectImageUpload.click()
    },
    async handleProjectImageUpload(event) {
      const file = event.target.files[0]
      if (!file) return
        // 验证文件类型
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
        if (!allowedTypes.includes(file.type)) {
          alert('只支持以下图片格式: jpg, png, gif, webp, bmp')
        this.$refs.projectImageUpload.value = ''
          return
        }
        // 验证文件大小（5MB）
        if (file.size > 5 * 1024 * 1024) {
          alert('文件大小不能超过5MB')
        this.$refs.projectImageUpload.value = ''
          return
        }
      // 创建预览URL并立即进入裁切模式
      const reader = new FileReader()
      reader.onload = (e) => {
        this.originalImageData = e.target.result
        // 立即显示裁切模态，用户必须完成裁切
        this.showCropModal = true
        this.$nextTick(() => {
          this.initCropCanvas()
        })
      }
      reader.readAsDataURL(file)
    },
    async uploadCroppedImage(imageDataUrl) {
      try {
        // 将DataURL转换为Blob
        const blob = await this.dataURLToBlob(imageDataUrl)
        // 导入项目API
        const { projectAPI } = await import('@/api/project')
        console.log('[uploadCroppedImage] 开始上传裁切后的项目图片')
        // 上传图片
        const response = await projectAPI.uploadProjectImage(blob, this.project.id)
        console.log('[uploadCroppedImage] API返回结果:', response)
        console.log('[uploadCroppedImage] 返回结果类型:', typeof response)
        console.log('[uploadCroppedImage] 返回结果完整信息:', JSON.stringify(response, null, 2))
        // ✅ 调试：打印响应的所有字段
        if (response) {
          console.log('[uploadCroppedImage] response.code:', response.code)
          console.log('[uploadCroppedImage] response.msg:', response.msg)
          console.log('[uploadCroppedImage] response.data:', response.data)
          if (response.data) {
            console.log('[uploadCroppedImage] response.data.imageUrl:', response.data.imageUrl)
            console.log('[uploadCroppedImage] response.data所有字段:', Object.keys(response.data))
          }
        }
        if (response && response.code === 200 && response.data) {
          // 更新项目图片URL
          const rawUrl = response.data.imageUrl
          console.log('[uploadCroppedImage] 提取到的imageUrl:', rawUrl)
          if (!rawUrl) {
            console.warn('[uploadCroppedImage] ⚠️ 警告：imageUrl 为空或不存在！')
            console.warn('[uploadCroppedImage] response.data 的所有字段:', response.data)
            alert('上传成功但未获取到图片URL')
            return
          }
          // 规范化后端返回的 URL
          const normalizedUrl = normalizeProjectCoverUrl(rawUrl)

          // ✅ 使用裁剪后的 dataURL 作为当前页面立即显示的图片，避免闪现旧图
          this.$set(this.project, 'image', imageDataUrl || normalizedUrl)
          this.$set(this.project, 'imageUrl', normalizedUrl || rawUrl)

          console.log('[uploadCroppedImage] 项目对象已更新为新封面:', {
            imageUrl: this.project.imageUrl,
            image: this.project.image
          })

          // ✅ 覆盖本地封面缓存，使后续进入广场/详情时直接使用新图
          try {
            if (this.project && this.project.id) {
              saveProjectCover(this.project.id, imageDataUrl || normalizedUrl, normalizedUrl || rawUrl)
            }
          } catch (e) {
            console.warn('[uploadCroppedImage] 覆盖封面缓存失败:', e)
          }

          // ✅ 同步更新 localStorage('projects') 中对应项目的图片字段
          try {
            const savedProjects = localStorage.getItem('projects')
            if (savedProjects) {
              const projectList = JSON.parse(savedProjects)
              if (Array.isArray(projectList)) {
                const updatedList = projectList.map(p => {
                  if (!p || String(p.id) !== String(this.project.id)) return p
                  return {
                    ...p,
                    image: imageDataUrl || normalizedUrl || rawUrl,
                    imageUrl: normalizedUrl || rawUrl
                  }
                })
                localStorage.setItem('projects', JSON.stringify(updatedList))
              }
            }
          } catch (e) {
            console.warn('[uploadCroppedImage] 更新 projects 缓存失败:', e)
          }

          // ✅ 更新项目详情缓存 project_detail_<id>
          try {
            const projectId = this.project?.id
            if (projectId) {
              const cacheKey = `project_detail_${projectId}`
              const raw = localStorage.getItem(cacheKey)
              if (raw) {
                const parsed = JSON.parse(raw)
                if (parsed && parsed.data && parsed.data.project) {
                  parsed.data.project = {
                    ...parsed.data.project,
                    image: imageDataUrl || normalizedUrl || rawUrl,
                    imageUrl: normalizedUrl || rawUrl
                  }
                  localStorage.setItem(cacheKey, JSON.stringify(parsed))
                }
              }
            }
          } catch (e) {
            console.warn('[uploadCroppedImage] 更新 project_detail 缓存失败:', e)
          }

          // 保存到localStorage（保持原有逻辑，写入 this.project 及相关字段）
          this.saveProjectData()

          // ✅ 通过根实例广播项目封面更新事件，通知项目广场等页面立即更新
          try {
            const projectId = this.project?.id
            if (projectId && this.$root && this.$root.$emit) {
              this.$root.$emit('projectCoverUpdated', {
                projectId,
                image: imageDataUrl || normalizedUrl || rawUrl,
                imageUrl: normalizedUrl || rawUrl
              })
            }
          } catch (e) {
            console.warn('[uploadCroppedImage] 发送 projectCoverUpdated 事件失败:', e)
          }

          // ✅ 强制Vue重新渲染（以防万一）
          this.$forceUpdate()
          console.log('[uploadCroppedImage] ✅ 强制更新Vue视图')
          console.log('[uploadCroppedImage] ✅ 图片URL已设置并缓存:', normalizedUrl || rawUrl)
          this.showSuccessToast('项目图片上传成功！')
          console.log('[uploadCroppedImage] 项目图片上传成功，新URL:', normalizedUrl || rawUrl)
        } else {
          console.error('[uploadCroppedImage] ❌ 响应不符合预期')
          console.error('[uploadCroppedImage] response:', response)
          alert('上传失败: ' + (response?.msg || '未知错误'))
        }
      } catch (error) {
        console.error('[uploadCroppedImage] 上传项目图片失败:', error)
        // ✅ 增强的错误处理
        if (error && error.status === 403) {
          console.error('[uploadCroppedImage] ❌ 403 Forbidden - 认证失败')
          console.error('[uploadCroppedImage] 可能原因: JWT token 过期或无效')
          const token = localStorage.getItem('access_token')
          if (!token) {
            console.error('[uploadCroppedImage] 🔴 Token 为空')
            alert('登录已过期，请重新登录')
          } else {
            alert('认证失败，请重新登录')
          }
        } else {
          alert('上传项目图片失败，请稍后重试')
        }
      }
    },
    closeCropModal() {
      // 如果用户取消裁切，清空文件输入
          this.$refs.projectImageUpload.value = ''
      this.showCropModal = false
      this.originalImage = null
      this.originalImageData = null
    },
    initCropCanvas() {
      const canvas = this.$refs.cropCanvas
      const ctx = canvas.getContext('2d')
      const img = new Image()
      img.onload = () => {
        // 设置画布尺寸，保持图片比例
        const maxWidth = 600
        const maxHeight = 400
        let { width, height } = img
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width *= ratio
          height *= ratio
        }
        canvas.width = width
        canvas.height = height
        canvas.style.width = width + 'px'
        canvas.style.height = height + 'px'
        // 绘制图片
        ctx.drawImage(img, 0, 0, width, height)
        // 保存原始图片数据
        this.originalImage = img
        // 初始化裁切区域（项目广场比例：约16:9）
        const cropRatio = 16 / 9
        const cropWidth = Math.min(width * 0.8, height * cropRatio * 0.8)
        const cropHeight = cropWidth / cropRatio
        this.cropData = {
          x: (width - cropWidth) / 2,
          y: (height - cropHeight) / 2,
          width: cropWidth,
          height: cropHeight
        }
        this.updateCropSelection()
        this.setupCropInteraction()
      }
      img.src = this.originalImageData
    },
    updateCropSelection() {
      const selection = this.$refs.cropSelection
      if (selection) {
        selection.style.left = this.cropData.x + 'px'
        selection.style.top = this.cropData.y + 'px'
        selection.style.width = this.cropData.width + 'px'
        selection.style.height = this.cropData.height + 'px'
      }
    },
    setupCropInteraction() {
      const selection = this.$refs.cropSelection
      const overlay = this.$refs.cropOverlay
      const canvas = this.$refs.cropCanvas
      if (!selection || !overlay || !canvas) return
      let isDragging = false
      let isResizing = false
      let resizeHandle = null
      let startX = 0
      let startY = 0
      let startCropX = 0
      let startCropY = 0
      let startCropWidth = 0
      let startCropHeight = 0
      const cropRatio = 16 / 9
      const startDrag = (e) => {
        if (e.target.classList.contains('resize-handle')) {
          isResizing = true
          resizeHandle = e.target.dataset.handle
        } else {
          isDragging = true
        }
        const rect = canvas.getBoundingClientRect()
        startX = e.clientX - rect.left
        startY = e.clientY - rect.top
        startCropX = this.cropData.x
        startCropY = this.cropData.y
        startCropWidth = this.cropData.width
        startCropHeight = this.cropData.height
      }
      const drag = (e) => {
        if (!isDragging && !isResizing) return
        const rect = canvas.getBoundingClientRect()
        const currentX = e.clientX - rect.left
        const currentY = e.clientY - rect.top
        const deltaX = currentX - startX
        const deltaY = currentY - startY
        if (isDragging) {
          // 移动裁切框
          const newX = Math.max(0, Math.min(canvas.width - this.cropData.width, startCropX + deltaX))
          const newY = Math.max(0, Math.min(canvas.height - this.cropData.height, startCropY + deltaY))
          this.cropData.x = newX
          this.cropData.y = newY
        } else if (isResizing) {
          // 调整裁切框大小
          let newWidth = startCropWidth
          let newHeight = startCropHeight
          let newX = startCropX
          let newY = startCropY
          if (resizeHandle === 'se') {
            // 右下角调整
            newWidth = Math.max(50, Math.min(canvas.width - startCropX, startCropWidth + deltaX))
            newHeight = newWidth / cropRatio
            if (startCropY + newHeight > canvas.height) {
              newHeight = canvas.height - startCropY
              newWidth = newHeight * cropRatio
            }
          } else if (resizeHandle === 'sw') {
            // 左下角调整
            newWidth = Math.max(50, Math.min(startCropX + startCropWidth, startCropWidth - deltaX))
            newHeight = newWidth / cropRatio
            newX = startCropX + startCropWidth - newWidth
            if (newX < 0) {
              newX = 0
              newWidth = startCropX + startCropWidth
              newHeight = newWidth / cropRatio
            }
          } else if (resizeHandle === 'ne') {
            // 右上角调整
            newWidth = Math.max(50, Math.min(canvas.width - startCropX, startCropWidth + deltaX))
            newHeight = newWidth / cropRatio
            newY = startCropY + startCropHeight - newHeight
            if (newY < 0) {
              newY = 0
              newHeight = startCropY + startCropHeight
              newWidth = newHeight * cropRatio
            }
          } else if (resizeHandle === 'nw') {
            // 左上角调整
            newWidth = Math.max(50, Math.min(startCropX + startCropWidth, startCropWidth - deltaX))
            newHeight = newWidth / cropRatio
            newX = startCropX + startCropWidth - newWidth
            newY = startCropY + startCropHeight - newHeight
            if (newX < 0) {
              newX = 0
              newWidth = startCropX + startCropWidth
              newHeight = newWidth / cropRatio
              newY = startCropY + startCropHeight - newHeight
            }
            if (newY < 0) {
              newY = 0
              newHeight = startCropY + startCropHeight
              newWidth = newHeight * cropRatio
              newX = startCropX + startCropWidth - newWidth
            }
          }
          this.cropData.x = newX
          this.cropData.y = newY
          this.cropData.width = newWidth
          this.cropData.height = newHeight
        }
        this.updateCropSelection()
      }
      const endDrag = () => {
        isDragging = false
        isResizing = false
        resizeHandle = null
      }
      selection.addEventListener('mousedown', startDrag)
      document.addEventListener('mousemove', drag)
      document.addEventListener('mouseup', endDrag)
      // 清理事件监听器
      this.$once('hook:beforeDestroy', () => {
        selection.removeEventListener('mousedown', startDrag)
        document.removeEventListener('mousemove', drag)
        document.removeEventListener('mouseup', endDrag)
      })
    },
    applyCrop() {
      if (!this.originalImage) return
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      // 设置裁切后的画布尺寸（项目广场比例）
      const targetRatio = 16 / 9
      const targetWidth = 400
      const targetHeight = targetWidth / targetRatio
      canvas.width = targetWidth
      canvas.height = targetHeight
      // 计算裁切区域在原图中的位置和尺寸
      const sourceX = (this.cropData.x / this.$refs.cropCanvas.width) * this.originalImage.width
      const sourceY = (this.cropData.y / this.$refs.cropCanvas.height) * this.originalImage.height
      const sourceWidth = (this.cropData.width / this.$refs.cropCanvas.width) * this.originalImage.width
      const sourceHeight = (this.cropData.height / this.$refs.cropCanvas.height) * this.originalImage.height
      // 绘制裁切后的图片
      ctx.drawImage(
        this.originalImage,
        sourceX, sourceY, sourceWidth, sourceHeight,
        0, 0, targetWidth, targetHeight
      )
      // 转换为DataURL并上传
      const imageDataUrl = canvas.toDataURL('image/jpeg', 0.9)
      this.closeCropModal()
      // 上传裁切后的图片
      this.uploadCroppedImage(imageDataUrl)
    },
    dataURLToBlob(dataURL) {
      return new Promise((resolve, reject) => {
        try {
          const arr = dataURL.split(',')
          const mime = arr[0].match(/:(.*?);/)[1]
          const bstr = atob(arr[1])
          let n = bstr.length
          const u8arr = new Uint8Array(n)
          while (n--) {
            u8arr[n] = bstr.charCodeAt(n)
          }
          const blob = new Blob([u8arr], { type: mime })
          resolve(blob)
        } catch (error) {
          reject(error)
        }
      })
    },
    showSuccessToast(message) {
      this.toastMessage = message
      this.showToast = true
      // 1秒后自动隐藏
      setTimeout(() => {
        this.showToast = false
        this.toastMessage = ''
      }, 1000)
    },

    /**
     * 申请加入当前项目（非成员在团队成员区域点击）
     */
    async applyJoinProject() {
      const projectId = this.project?.id || this.$route.params.id
      if (!projectId) {
        this.showSuccessToast('项目信息异常，无法申请加入')
        return
      }

      if (!this.getCurrentUserId()) {
        this.showSuccessToast('请先登录后再申请加入项目')
        return
      }

      try {
        const reason = window.prompt(`申请加入项目「${this.project?.name || this.project?.title || ''}」的理由（可选）：`, '')
        const { projectAPI } = await import('@/api/project')
        const res = await projectAPI.applyToJoinProject(projectId, reason || '')

        if (res && res.code === 200) {
          this.showSuccessToast(res.msg || '申请已发送，等待管理员处理')
        } else {
          this.showSuccessToast(res?.msg || '申请失败，请稍后重试')
        }
      } catch (error) {
        console.error('申请加入项目失败:', error)
        this.showSuccessToast('申请失败: ' + (error.message || '网络错误'))
      }
    },
    onImageLoad() {
      console.log('✅ 图片加载成功:', this.project.imageUrl || this.project.image)
    },
    onImageError(event) {
      const imageUrl = this.project.imageUrl || this.project.image
      // 使用 console.warn 而不是 console.error，避免触发全局错误处理
      console.warn('⚠️ 图片加载失败，URL:', imageUrl)
      console.warn('⚠️ 可能的原因：')
      console.warn('  1. CORS 跨域问题 - MinIO 没有正确配置 CORS')
      console.warn('  2. 图片 URL 不正确')
      console.warn('  3. MinIO 服务不可用或桶策略未设置为public')
      console.warn('  4. 网络连接问题')
      // 测试URL可访问性
      if (imageUrl) {
        console.log('📝 测试 URL 可访问性，请在浏览器中直接访问：', imageUrl)
        console.log('📝 或运行以下PowerShell命令测试：')
        console.log(`   Invoke-WebRequest -Uri "${imageUrl}" -Method GET -UseBasicParsing`)
        // 尝试fetch测试（使用 try-catch 包装，避免未捕获的错误）
        fetch(imageUrl, { method: 'HEAD' })
          .then(response => {
            console.log('✅ HEAD请求成功，状态码:', response.status)
            console.log('✅ Content-Type:', response.headers.get('Content-Type'))
            console.log('✅ Access-Control-Allow-Origin:', response.headers.get('Access-Control-Allow-Origin'))
          })
          .catch(error => {
            // 使用 console.warn 而不是 console.error，避免触发全局错误处理
            console.warn('⚠️ HEAD请求失败:', error.message || error)
            console.warn('⚠️ 这通常表示CORS或网络问题')
          })
      }
    },
    /**
     * 解析头像URL
     * 后端返回的avatar可能是JSON字符串格式: {"sizes":{"256":"http://...","original":"http://..."}}
     * 需要解析并提取original尺寸的URL
     */
    parseAvatarUrl(avatar) {
      if (!avatar) {
        return null
      }
      // 如果已经是普通URL字符串，直接返回
      if (typeof avatar === 'string' && (avatar.startsWith('http://') || avatar.startsWith('https://'))) {
        return addTimestampToUrl(avatar)
      }
      // 如果是JSON字符串，尝试解析
      if (typeof avatar === 'string') {
        try {
          const avatarObj = JSON.parse(avatar)
          // 优先使用original尺寸，其次使用256尺寸
          if (avatarObj.sizes) {
            const rawUrl = avatarObj.sizes.original || avatarObj.sizes['256'] || avatarObj.sizes['512'] || null
            return rawUrl ? addTimestampToUrl(rawUrl) : null
          }
          return null
        } catch (e) {
          console.warn('解析头像URL失败:', e, '原始数据:', avatar)
          return null
        }
      }
      // 如果是对象，直接提取URL
      if (typeof avatar === 'object' && avatar.sizes) {
        const rawUrl = avatar.sizes.original || avatar.sizes['256'] || avatar.sizes['512'] || null
        return rawUrl ? addTimestampToUrl(rawUrl) : null
      }
      return null
    },
    /**
     * 从角色名称获取角色代码
     */
    getRoleCodeFromName(roleName) {
      if (!roleName) return 'MEMBER'
      const roleUpper = String(roleName).toUpperCase()
      // 优先检查中文
      if (roleUpper.includes('拥有者') || roleUpper.includes('负责人')) {
        return 'OWNER'
      }
      if (roleUpper.includes('管理员')) {
        return 'ADMIN'
      }
      // 检查英文
      if (roleUpper.includes('OWNER')) {
        return 'OWNER'
      }
      if (roleUpper.includes('ADMIN')) {
        return 'ADMIN'
      }
      // 检查成员
      if (roleUpper.includes('成员') || roleUpper === 'MEMBER') {
        return 'MEMBER'
      }
      return 'MEMBER'
    },
    /**
     * 获取角色显示名称
     */
    getRoleDisplayName(roleCodeOrName) {
      if (!roleCodeOrName) return '项目成员'
      const role = String(roleCodeOrName).toUpperCase()
      // 检查中文角色名
      if (role.includes('拥有者') || role.includes('负责人') || role === 'OWNER') {
        return '项目拥有者'
      }
      if (role.includes('管理员') || role === 'ADMIN') {
        return '项目管理员'
      }
      // 默认返回原始角色名，如果包含"成员"则返回"项目成员"
      if (role.includes('成员') || role === 'MEMBER') {
        return '项目成员'
      }
      return roleCodeOrName || '项目成员'
    },
    /**
     * 获取角色徽章样式类
     */
    getRoleBadgeClass(roleCodeOrName) {
      if (!roleCodeOrName) return 'member'
      const role = String(roleCodeOrName).toUpperCase()
      if (role === 'OWNER' || role.includes('拥有者') || role.includes('负责人')) {
        return 'owner'
      }
      if (role === 'ADMIN' || role.includes('管理员')) {
        return 'admin'
      }
      return 'member'
    },
    /**
     * 判断成员是否为拥有者
     */
    isOwnerMember(member) {
      if (!member) return false
      const roleCode = String(member.roleCode || '').toUpperCase()
      const roleName = String(member.role || '').toUpperCase()
      // 检查角色代码
      if (roleCode === 'OWNER') {
        return true
      }
      // 检查角色名称（中文）
      if (roleName.includes('拥有者') || roleName.includes('负责人')) {
        return true
      }
      // 检查角色名称（英文）
      if (roleName.includes('OWNER')) {
        return true
      }
      return false
    },
    /**
     * 判断成员是否为管理员
     */
    isAdminRole(member) {
      if (!member) return false
      const roleCode = String(member.roleCode || '').toUpperCase()
      const roleName = String(member.role || '').toUpperCase()
      // 检查角色代码
      if (roleCode === 'ADMIN') {
        return true
      }
      // 检查角色名称（中文）
      if (roleName.includes('管理员')) {
        return true
      }
      // 检查角色名称（英文）
      if (roleName.includes('ADMIN')) {
        return true
      }
      return false
    },
    /**
     * 判断是否可以移除成员
     */
    canRemoveMember(member) {
      if (!member) return false
      // 不能移除项目拥有者
      if (this.isOwnerMember(member)) {
        return false
      }
      // 如果当前用户是OWNER，可以移除任何成员（除了OWNER）
      if (this.isProjectOwner) {
        return true
      }
      // 如果当前用户是ADMIN，只能移除MEMBER，不能移除其他ADMIN
      if (this.isProjectManager && !this.isProjectOwner) {
        return !this.isAdminRole(member)
      }
      return false
    },
    /**
     * 切换成员角色下拉菜单
     */
    toggleMemberRoleDropdown(memberId) {
      // 关闭其他成员的下拉菜单
      Object.keys(this.memberRoleDropdownOpen).forEach(id => {
        if (String(id) !== String(memberId)) {
          this.$set(this.memberRoleDropdownOpen, id, false)
        }
      })
      // 切换当前成员的下拉菜单
      this.$set(this.memberRoleDropdownOpen, memberId, !this.memberRoleDropdownOpen[memberId])
    },
    /**
     * 设置成员角色
     */
    async setMemberRole(member, newRole) {
      // 关闭下拉菜单
      this.$set(this.memberRoleDropdownOpen, member.id, false)
      // 检查权限：只有OWNER可以设置管理员
      if (newRole === 'ADMIN' && !this.isProjectOwner) {
        this.showSuccessToast('只有项目拥有者可以设置管理员')
        return
      }
      // 不能修改项目拥有者的角色
      if (this.isOwnerMember(member)) {
        this.showSuccessToast('不能修改项目拥有者的角色')
        return
      }
      // 不能修改自己的角色
      if (this.isCurrentUser(member)) {
        this.showSuccessToast('不能修改自己的角色')
        return
      }
      // ADMIN不能修改其他ADMIN的角色，只有OWNER可以
      if (this.isAdminRole(member) && newRole !== 'ADMIN' && !this.isProjectOwner) {
        this.showSuccessToast('管理员不能修改其他管理员的角色，只有项目拥有者可以')
        return
      }
      // 显示确认弹窗
      this.memberToChangeRole = member
      this.newRoleToSet = newRole
      this.roleChangeTitle = newRole === 'ADMIN' ? '设置管理员' : '移除管理员'
      this.roleChangeMessage = newRole === 'ADMIN' 
        ? `确定要将 ${member.name} 设为项目管理员吗？` 
        : `确定要移除 ${member.name} 的管理员身份吗？`
      this.roleChangeConfirmOpen = true
    },
    /**
     * 取消角色变更
     */
    cancelRoleChange() {
      this.roleChangeConfirmOpen = false
      this.memberToChangeRole = null
      this.newRoleToSet = null
      this.roleChangeTitle = ''
      this.roleChangeMessage = ''
    },
    /**
     * 确认角色变更
     */
    async confirmRoleChange() {
      const member = this.memberToChangeRole
      const newRole = this.newRoleToSet
      
      // 关闭弹窗
      this.roleChangeConfirmOpen = false
      
      if (!member || !newRole) {
        return
      }
      
      try {
        const { projectAPI } = await import('@/api/project')
        const projectId = this.$route.params.id
        // 使用userId（member.id和member.userId在loadTeamMembers中都被设置为userId）
        const userId = member.userId || member.id
        const response = await projectAPI.updateMemberRole(projectId, userId, newRole)
        if (response && response.code === 200) {
          // 更新成功，重新加载团队成员列表
          await this.loadTeamMembers()
          // 重新检查权限（因为角色可能影响权限）
          await this.checkAdminPermission()
          const successMsg = newRole === 'ADMIN' 
            ? `已成功将 ${member.name} 设为项目管理员` 
            : `已成功移除 ${member.name} 的管理员身份`
          this.showSuccessToast(successMsg)
        } else {
          const errorMsg = response?.msg || response?.message || '更新角色失败'
          this.showSuccessToast(errorMsg)
        }
      } catch (error) {
        console.error('更新成员角色失败:', error)
        const errorMsg = error?.msg || error?.message || '网络错误'
        this.showSuccessToast('更新角色失败: ' + errorMsg)
      } finally {
        // 清理状态
        this.memberToChangeRole = null
        this.newRoleToSet = null
        this.roleChangeTitle = ''
        this.roleChangeMessage = ''
      }
    },
    /**
     * 处理任务状态更新事件
     */
    handleTaskStatusUpdated() {
      // 重新加载任务列表
      this.loadProjectTasks()
    },
    /**
     * 判断成员是否为当前用户
     */
    isCurrentUser(member) {
      if (!member) return false
      const currentUserId = this.getCurrentUserId()
      if (!currentUserId) return false
      const memberId = String(member.id || member.userId || '')
      const userId = String(currentUserId)
      return memberId === userId
    },
    async fetchTaskWorktime(taskId) {
      if (!taskId) {
        this.selectedTaskWorktimeLoading = false
        return
      }
      const targetId = String(taskId)
      this.selectedTaskWorktimeLoading = true
      try {
        const response = await getLatestSubmission(taskId)
        if (response && response.code === 200 && response.data) {
          const submission = response.data
          if (submission.actualWorktime !== undefined && submission.actualWorktime !== null) {
            const info = {
              value: submission.actualWorktime,
              submitter: this.getSubmissionSubmitterName(submission),
              submittedAt: this.formatDateTime(
                submission.submissionTime ||
                submission.submittedAt ||
                submission.submission_time ||
                submission.createdAt ||
                submission.created_at
              )
            }
            if (this.selectedTask && String(this.selectedTask.id) === targetId) {
              this.selectedTaskWorktimeInfo = info
            }
          } else if (this.selectedTask && String(this.selectedTask.id) === targetId) {
            this.selectedTaskWorktimeInfo = null
          }
        } else if (this.selectedTask && String(this.selectedTask.id) === targetId) {
          this.selectedTaskWorktimeInfo = null
        }
      } catch (error) {
        console.error('[fetchTaskWorktime] 加载工时信息失败', error)
        if (this.selectedTask && String(this.selectedTask.id) === targetId) {
          this.selectedTaskWorktimeInfo = null
        }
      } finally {
        if (this.selectedTask && String(this.selectedTask.id) === targetId) {
          this.selectedTaskWorktimeLoading = false
        }
      }
    },
    getSubmissionSubmitterName(submission) {
      if (!submission) return '未知用户'
      if (submission.submitter) {
        const submitter = submission.submitter
        const name = submitter.name || submitter.username || submitter.userName || submitter.nickname
        if (name && String(name).trim() !== '' && name !== '未知用户') {
          return name
        }
      }
      const directFields = [
        submission.submitterName,
        submission.submitter_name,
        submission.submitterUsername,
        submission.submitter_username
      ]
      for (const field of directFields) {
        if (field && String(field).trim() !== '' && field !== '未知用户') {
          return field
        }
      }
      return '未知用户'
    },
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      if (Number.isNaN(date.getTime())) {
        return String(dateTime)
      }
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    }
  }
}
</script>

<style scoped>
/* 任务状态下拉框按钮禁用样式 */
.task-status-btn.disabled,
.task-status-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f3f4f6 !important;
  color: #9ca3af !important;
}

.task-status-btn.disabled:hover,
.task-status-btn:disabled:hover {
  transform: none;
  filter: none;
  opacity: 0.6;
}

/* 任务列表底部"更多"按钮靠左对齐 */
.more-button-container {
  display: flex;
  justify-content: flex-start;
  margin-top: 16px;
  padding-left: 0;
}

/* 任务已满员状态样式 */
.assign-status-badge.task-full {
  background-color: #fef3c7;
  color: #92400e;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

/* 任务参与人数显示样式 */
.task-participant-count {
  color: #6b7280;
  font-size: 12px;
}

/* 任务元信息区域 - 确保换行显示 */
.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

/* 任务操作区域 - 独立一行，不与上方文本重叠 */
.task-assign-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  clear: both;
}

/* 分配任务模态框 - 当前执行者状态 */
.task-assignee-status {
  margin-top: 12px;
  padding: 12px;
  background-color: #f3f4f6;
  border-radius: 6px;
  font-size: 14px;
}

.task-assignee-status .status-label {
  font-weight: 500;
  color: #374151;
  margin-right: 8px;
}

.task-assignee-status .status-value {
  color: #6b7280;
}

.task-assignee-status .status-count {
  color: #10b981;
  font-weight: 500;
  margin-left: 4px;
}

/* 已分配成员样式 */
.member-select-item.already-assigned {
  background-color: #f0fdf4;
  border-color: #86efac;
}

.already-assigned-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  background-color: #10b981;
  color: white;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

/* 任务详情模态框按钮样式 */
.btn-success {
  background-color: #10b981;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-success:hover {
  background-color: #059669;
  transform: translateY(-1px);
}

.btn-success:active {
  transform: translateY(0);
}

/* 接取人数显示样式 */
.text-success {
  color: #10b981;
  font-weight: 600;
}

.text-warning {
  color: #f59e0b;
  font-weight: 600;
}

.task-full-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  background-color: #fef3c7;
  color: #92400e;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.overdue-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  background-color: #fee2e2;
  color: #991b1b;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.task-info-icon.participants {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

/* 统计信息图标样式 */
.task-info-icon.statistics {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

/* 统计信息网格布局 */
.task-statistics-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.stat-label {
  color: #6b7280;
  font-weight: 400;
}

.stat-value {
  color: #1f2937;
  font-weight: 600;
  font-size: 14px;
}

/* 可点击的卡片样式 */
.task-info-card.clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.task-info-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.click-hint {
  margin-left: 8px;
  font-size: 11px;
  color: #9ca3af;
  font-weight: 400;
}

/* 统计详情弹窗样式 */
.statistics-modal {
  max-width: 700px;
  max-height: 85vh;
  overflow-y: auto;
}

.statistics-modal-body {
  padding: 24px;
}

.statistics-task-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e5e7eb;
}

.statistics-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.section-header svg {
  color: #8b5cf6;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0;
}

/* 统计概览卡片 */
.stats-overview {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.stat-card-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-card-value {
  font-size: 28px;
  font-weight: 700;
  color: #8b5cf6;
}

.stat-card-value.approved {
  color: #10b981;
}

/* 执行者列表样式 */
.assignees-list {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
}

.assignee-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assignee-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: white;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.assignee-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 头像图片容器 */
.assignee-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background-color: #f3f4f6;
}

/* 头像图片 */
.assignee-avatar-img .avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.assignee-info {
  flex: 1;
}

.assignee-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.assignee-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.assignee-type {
  padding: 2px 8px;
  background-color: #ddd6fe;
  color: #6d28d9;
  border-radius: 4px;
  font-weight: 500;
}

.assignee-time {
  color: #9ca3af;
}

/* 提交详情样式 */
.submission-details {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
}

.submission-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.meta-label {
  color: #6b7280;
  font-weight: 500;
}

.meta-value {
  color: #1f2937;
  font-weight: 600;
}

.status-approved {
  color: #10b981 !important;
}

.submission-content {
  margin-bottom: 16px;
}

.content-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.content-text {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  background-color: white;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  white-space: pre-wrap;
}

.submission-attachments {
  margin-top: 16px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background-color: white;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.attachment-item:hover {
  background-color: #f3f4f6;
  border-color: #8b5cf6;
}

.attachment-item svg {
  color: #8b5cf6;
  flex-shrink: 0;
}

.attachment-name {
  font-size: 13px;
  color: #4b5563;
  text-decoration: none;
  flex: 1;
}

.attachment-name:hover {
  color: #8b5cf6;
  text-decoration: underline;
}

.empty-state {
  text-align: center;
  padding: 32px;
  color: #9ca3af;
  font-size: 14px;
}

/* 里程碑开关样式 */
.form-field-switch {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}

.switch-toggle {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
}

.switch-toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.switch-slider {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #e5e7eb;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.switch-slider::before {
  position: absolute;
  content: "";
  height: 20px;
  width: 20px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.switch-toggle input:checked + .switch-slider {
  background-color: #3b82f6;
}

.switch-toggle input:checked + .switch-slider::before {
  transform: translateX(20px);
}

.switch-toggle input:focus + .switch-slider {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

/* 里程碑任务卡片样式 */
.milestone-card {
  grid-column: span 2;
}

.milestone-card .task-info-icon.milestone {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.milestone-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.milestone-switch-wrapper {
  display: flex;
  align-items: center;
}

.milestone-status-text {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}
</style>
