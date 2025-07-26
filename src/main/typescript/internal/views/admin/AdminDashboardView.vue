<template>
  <div
    class="min-h-screen bg-gradient-to-br from-background via-muted/30 to-accent/5"
  >
    <div class="container mx-auto p-6 space-y-6">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between">
        <div>
          <h1
            class="text-3xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent"
          >
            管理仪表板
          </h1>
          <p class="text-muted-foreground mt-1">系统概览和监控数据</p>
        </div>
        <div class="flex items-center gap-2">
          <Badge :variant="systemHealthVariant" class="px-3 py-1">
            <div
              class="w-2 h-2 rounded-full mr-2 animate-pulse"
              :class="systemHealthColor"
            ></div>
            {{ systemHealthLabel }}
          </Badge>
          <Button
            @click="refreshData"
            variant="outline"
            size="sm"
            :disabled="loading"
          >
            <RefreshCw
              class="w-4 h-4 mr-2"
              :class="{ 'animate-spin': loading }"
            />
            刷新数据
          </Button>
        </div>
      </div>

      <!-- 关键指标卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <!-- 总用户数 -->
        <Card
          class="border-border/50 shadow-lg hover:shadow-xl transition-all duration-300 group"
        >
          <CardContent class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-muted-foreground">
                  总用户数
                </p>
                <p
                  class="text-3xl font-bold text-tech-blue group-hover:scale-105 transition-transform"
                >
                  {{ dashboardData?.overview.totalUsers.toLocaleString() || 0 }}
                </p>
                <p class="text-sm text-muted-foreground mt-1">
                  <TrendingUp class="w-4 h-4 inline mr-1 text-success" />
                  本月新增 {{ userStatistics?.newUsersThisMonth || 0 }}
                </p>
              </div>
              <div class="p-3 bg-tech-blue/10 rounded-full">
                <Users class="w-8 h-8 text-tech-blue" />
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 总文件数 -->
        <Card
          class="border-border/50 shadow-lg hover:shadow-xl transition-all duration-300 group"
        >
          <CardContent class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-muted-foreground">
                  总文件数
                </p>
                <p
                  class="text-3xl font-bold text-tech-purple group-hover:scale-105 transition-transform"
                >
                  {{ dashboardData?.overview.totalFiles.toLocaleString() || 0 }}
                </p>
                <p class="text-sm text-muted-foreground mt-1">
                  <Upload class="w-4 h-4 inline mr-1 text-success" />
                  今日上传 {{ fileStatistics?.filesUploadedToday || 0 }}
                </p>
              </div>
              <div class="p-3 bg-tech-purple/10 rounded-full">
                <FileText class="w-8 h-8 text-tech-purple" />
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 存储使用 -->
        <Card
          class="border-border/50 shadow-lg hover:shadow-xl transition-all duration-300 group"
        >
          <CardContent class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-muted-foreground">
                  存储使用
                </p>
                <p
                  class="text-3xl font-bold text-neon-green group-hover:scale-105 transition-transform"
                >
                  {{ storageStatistics?.usagePercentage.toFixed(1) || 0 }}%
                </p>
                <p class="text-sm text-muted-foreground mt-1">
                  {{ storageStatistics?.usedSpaceReadable || "0 B" }} /
                  {{ storageStatistics?.totalSpaceReadable || "0 B" }}
                </p>
              </div>
              <div class="p-3 bg-neon-green/10 rounded-full">
                <HardDrive class="w-8 h-8 text-neon-green" />
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 系统状态 -->
        <Card
          class="border-border/50 shadow-lg hover:shadow-xl transition-all duration-300 group"
        >
          <CardContent class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-muted-foreground">
                  系统运行时间
                </p>
                <p
                  class="text-3xl font-bold text-orange-500 group-hover:scale-105 transition-transform"
                >
                  {{ systemMetrics?.uptimeReadable || "0 day" }}
                </p>
                <p class="text-sm text-muted-foreground mt-1">
                  <Activity class="w-4 h-4 inline mr-1 text-success" />
                  CPU {{ systemMetrics?.cpuUsage.toFixed(1) || 0 }}%
                </p>
              </div>
              <div class="p-3 bg-orange-500/10 rounded-full">
                <Server class="w-8 h-8 text-orange-500" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 图表和统计 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 用户增长趋势 -->
        <Card class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <TrendingUp class="w-5 h-5 mr-2 text-tech-blue" />
              用户增长趋势
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div
              class="h-64 flex items-center justify-center"
              v-if="!userStatistics?.registrationTrend.length"
            >
              <p class="text-muted-foreground">暂无数据</p>
            </div>
            <div v-else class="h-64">
              <!-- 这里应该使用图表库如 Chart.js 或 echarts，这里用简化的可视化 -->
              <div class="space-y-2">
                <div
                  v-for="(
                    item, index
                  ) in userStatistics.registrationTrend.slice(-7)"
                  :key="index"
                  class="flex items-center justify-between"
                >
                  <span class="text-sm">{{ formatDate(item.date) }}</span>
                  <div class="flex items-center gap-2">
                    <div
                      class="bg-gradient-to-r from-tech-blue to-tech-purple rounded-full h-2"
                      :style="
                        getBarWidth(
                          item.count,
                          userStatistics.registrationTrend
                        )
                      "
                    ></div>
                    <span class="text-sm font-medium">{{ item.count }}</span>
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 文件类型分布 -->
        <Card class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <PieChart class="w-5 h-5 mr-2 text-tech-purple" />
              文件类型分布
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div class="space-y-4" v-if="fileStatistics?.filesByType">
              <div
                v-for="(typeData, type) in Object.entries(
                  fileStatistics.filesByType
                ).slice(0, 5)"
                :key="type"
                class="flex items-center justify-between"
              >
                <div class="flex items-center gap-2">
                  <div
                    class="w-3 h-3 rounded-full"
                    :style="{ backgroundColor: getTypeColor(String(type)) }"
                  ></div>
                  <span class="text-sm font-medium uppercase">{{ type }}</span>
                </div>
                <div class="text-right">
                  <div class="text-sm font-medium">
                    {{ typeData[1].count.toLocaleString() }}
                  </div>
                  <div class="text-xs text-muted-foreground">
                    {{ formatBytes(typeData[1].size) }}
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="h-32 flex items-center justify-center">
              <p class="text-muted-foreground">暂无数据</p>
            </div>
          </CardContent>
        </Card>

        <!-- 系统性能监控 -->
        <Card class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <Activity class="w-5 h-5 mr-2 text-neon-green" />
              系统性能
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div class="space-y-4" v-if="systemMetrics">
              <!-- CPU 使用率 -->
              <div class="space-y-2">
                <div class="flex justify-between text-sm">
                  <span>CPU 使用率</span>
                  <span class="font-medium"
                    >{{ systemMetrics.cpuUsage.toFixed(1) }}%</span
                  >
                </div>
                <div class="w-full bg-muted rounded-full h-2">
                  <div
                    class="bg-gradient-to-r from-tech-blue to-neon-green h-2 rounded-full transition-all"
                    :style="{ width: `${systemMetrics.cpuUsage}%` }"
                  ></div>
                </div>
              </div>

              <!-- 内存使用率 -->
              <div class="space-y-2">
                <div class="flex justify-between text-sm">
                  <span>内存使用率</span>
                  <span class="font-medium"
                    >{{ systemMetrics.memoryUsage.toFixed(1) }}%</span
                  >
                </div>
                <div class="w-full bg-muted rounded-full h-2">
                  <div
                    class="bg-gradient-to-r from-tech-purple to-neon-green h-2 rounded-full transition-all"
                    :style="{ width: `${systemMetrics.memoryUsage}%` }"
                  ></div>
                </div>
              </div>

              <!-- 磁盘使用率 -->
              <div class="space-y-2">
                <div class="flex justify-between text-sm">
                  <span>磁盘使用率</span>
                  <span class="font-medium"
                    >{{ systemMetrics.diskUsage.toFixed(1) }}%</span
                  >
                </div>
                <div class="w-full bg-muted rounded-full h-2">
                  <div
                    class="bg-gradient-to-r from-orange-500 to-neon-green h-2 rounded-full transition-all"
                    :style="{ width: `${systemMetrics.diskUsage}%` }"
                  ></div>
                </div>
              </div>

              <!-- 网络流量 -->
              <div class="grid grid-cols-2 gap-4 pt-2 border-t">
                <div class="text-center">
                  <div class="text-sm text-muted-foreground">网络入</div>
                  <div class="text-lg font-semibold text-tech-blue">
                    {{ formatBytes(systemMetrics.networkIn) }}/s
                  </div>
                </div>
                <div class="text-center">
                  <div class="text-sm text-muted-foreground">网络出</div>
                  <div class="text-lg font-semibold text-tech-purple">
                    {{ formatBytes(systemMetrics.networkOut) }}/s
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 最近活动 -->
        <Card class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <Clock class="w-5 h-5 mr-2 text-orange-500" />
              最近活动
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div
              class="space-y-3"
              v-if="dashboardData?.recentActivities.length"
            >
              <div
                v-for="activity in dashboardData.recentActivities.slice(0, 8)"
                :key="activity.timestamp"
                class="flex items-start gap-3 p-2 rounded-lg hover:bg-muted/50 transition-colors"
              >
                <div class="p-1 bg-primary/10 rounded-full mt-1">
                  <div class="w-2 h-2 bg-primary rounded-full"></div>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm">{{ activity.description }}</p>
                  <div class="flex items-center gap-2 mt-1">
                    <span class="text-xs text-muted-foreground">{{
                      activity.user
                    }}</span>
                    <span class="text-xs text-muted-foreground">•</span>
                    <span class="text-xs text-muted-foreground">{{
                      formatDateTime(activity.timestamp)
                    }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="h-32 flex items-center justify-center">
              <p class="text-muted-foreground">暂无活动记录</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 告警和通知 -->
      <Card
        class="border-border/50 shadow-lg"
        v-if="dashboardData?.alerts.length"
      >
        <CardHeader>
          <CardTitle class="flex items-center">
            <AlertTriangle class="w-5 h-5 mr-2 text-orange-500" />
            系统告警
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="space-y-3">
            <div
              v-for="alert in dashboardData.alerts"
              :key="alert.timestamp"
              class="flex items-start gap-3 p-3 rounded-lg border"
              :class="getAlertClasses(alert.type)"
            >
              <component
                :is="getAlertIcon(alert.type)"
                class="w-5 h-5 mt-0.5"
                :class="getAlertIconColor(alert.type)"
              />
              <div class="flex-1">
                <p class="font-medium">{{ alert.message }}</p>
                <p class="text-sm text-muted-foreground mt-1">
                  {{ formatDateTime(alert.timestamp) }}
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 快速操作 -->
      <Card class="border-border/50 shadow-lg">
        <CardHeader>
          <CardTitle class="flex items-center">
            <Zap class="w-5 h-5 mr-2 text-neon-green" />
            快速操作
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Button
              variant="outline"
              class="h-16 flex-col gap-2 hover:border-tech-blue hover:text-tech-blue"
              @click="$router.push('/admin/users')"
            >
              <Users class="w-6 h-6" />
              <span class="text-xs">用户管理</span>
            </Button>
            <Button
              variant="outline"
              class="h-16 flex-col gap-2 hover:border-tech-purple hover:text-tech-purple"
              @click="$router.push('/admin/config')"
            >
              <Settings class="w-6 h-6" />
              <span class="text-xs">系统配置</span>
            </Button>
            <Button
              variant="outline"
              class="h-16 flex-col gap-2 hover:border-neon-green hover:text-neon-green"
              @click="clearCache"
            >
              <Trash2 class="w-6 h-6" />
              <span class="text-xs">清理缓存</span>
            </Button>
            <Button
              variant="outline"
              class="h-16 flex-col gap-2 hover:border-orange-500 hover:text-orange-500"
              @click="$router.push('/admin/logs')"
            >
              <FileText class="w-6 h-6" />
              <span class="text-xs">系统日志</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { adminApi } from "@/apis/admin-mock";
import type {
  IDashboardData,
  IUserStatistics,
  IFileStatistics,
  IStorageStatistics,
  ISystemMetrics,
  IAlert,
} from "@/types/admin";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import {
  Users,
  FileText,
  HardDrive,
  Server,
  Activity,
  TrendingUp,
  Upload,
  PieChart,
  Clock,
  AlertTriangle,
  Zap,
  RefreshCw,
  Settings,
  Trash2,
  Info,
  AlertCircle,
  XCircle,
} from "lucide-vue-next";

// 响应式数据
const loading = ref(false);
const dashboardData = ref<IDashboardData | null>(null);
const userStatistics = ref<IUserStatistics | null>(null);
const fileStatistics = ref<IFileStatistics | null>(null);
const storageStatistics = ref<IStorageStatistics | null>(null);
const systemMetrics = ref<ISystemMetrics | null>(null);

// 定时刷新
let refreshInterval: number | null = null;

// 计算属性
const systemHealthVariant = computed(() => {
  const health = dashboardData.value?.overview.systemHealth;
  switch (health) {
    case "UP":
      return "default";
    case "DEGRADED":
      return "secondary";
    case "DOWN":
      return "destructive";
    default:
      return "outline";
  }
});

const systemHealthColor = computed(() => {
  const health = dashboardData.value?.overview.systemHealth;
  switch (health) {
    case "UP":
      return "bg-success";
    case "DEGRADED":
      return "bg-warning";
    case "DOWN":
      return "bg-destructive";
    default:
      return "bg-muted-foreground";
  }
});

const systemHealthLabel = computed(() => {
  const health = dashboardData.value?.overview.systemHealth;
  switch (health) {
    case "UP":
      return "系统正常";
    case "DEGRADED":
      return "性能降级";
    case "DOWN":
      return "系统异常";
    default:
      return "状态未知";
  }
});

// 方法
const loadDashboardData = async () => {
  try {
    dashboardData.value = await adminApi.getDashboardData();
  } catch (error) {
    console.error("加载仪表板数据失败:", error);
  }
};

const loadStatistics = async () => {
  try {
    const [users, files, storage, system] = await Promise.all([
      adminApi.getUserStatistics(),
      adminApi.getFileStatistics(),
      adminApi.getStorageStatistics(),
      adminApi.getSystemStatistics(),
    ]);

    userStatistics.value = users;
    fileStatistics.value = files;
    storageStatistics.value = storage;
    systemMetrics.value = system.system;
  } catch (error) {
    console.error("加载统计数据失败:", error);
  }
};

const refreshData = async () => {
  loading.value = true;
  try {
    await Promise.all([loadDashboardData(), loadStatistics()]);
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString("zh-CN", {
    month: "short",
    day: "numeric",
  });
};

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString("zh-CN");
};

const formatBytes = (bytes: number) => {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
};

const getTypeColor = (type: string) => {
  const colors = {
    pdf: "#ef4444",
    docx: "#3b82f6",
    xlsx: "#10b981",
    jpg: "#f59e0b",
    png: "#8b5cf6",
    txt: "#6b7280",
  };
  return colors[type as keyof typeof colors] || "#6b7280";
};

const getBarWidth = (count: number, trendData: Array<{ count: number }>) => {
  const maxCount = Math.max(...trendData.map((t) => t.count));
  const widthPx = Math.round((count / maxCount) * 200);
  return { width: `${widthPx}px` };
};

const getAlertIcon = (type: string) => {
  switch (type) {
    case "INFO":
      return Info;
    case "WARNING":
      return AlertCircle;
    case "ERROR":
      return XCircle;
    case "CRITICAL":
      return AlertTriangle;
    default:
      return Info;
  }
};

const getAlertIconColor = (type: string) => {
  switch (type) {
    case "INFO":
      return "text-blue-500";
    case "WARNING":
      return "text-orange-500";
    case "ERROR":
      return "text-red-500";
    case "CRITICAL":
      return "text-red-600";
    default:
      return "text-blue-500";
  }
};

const getAlertClasses = (type: string) => {
  switch (type) {
    case "INFO":
      return "border-blue-200 bg-blue-50/50";
    case "WARNING":
      return "border-orange-200 bg-orange-50/50";
    case "ERROR":
      return "border-red-200 bg-red-50/50";
    case "CRITICAL":
      return "border-red-300 bg-red-100/50";
    default:
      return "border-border bg-muted/50";
  }
};

const clearCache = async () => {
  if (confirm("确定要清理所有缓存吗？这可能会暂时影响系统性能。")) {
    try {
      await adminApi.clearAllCache();
      // 可以显示成功提示
    } catch (error) {
      console.error("清理缓存失败:", error);
    }
  }
};

// 生命周期
onMounted(() => {
  refreshData();

  // 设置定时刷新（每30秒）
  refreshInterval = setInterval(() => {
    refreshData();
  }, 30000);
});

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});
</script>

<style scoped>
/* 渐变动画 */
@keyframes gradient-shift {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.animate-gradient {
  background-size: 200% 200%;
  animation: gradient-shift 3s ease infinite;
}

/* 卡片悬停效果 */
.group:hover .group-hover\:scale-105 {
  transform: scale(1.05);
}

/* 进度条动画 */
.h-2.rounded-full {
  transition: width 0.5s ease-in-out;
}

/* 响应式网格调整 */
@media (max-width: 768px) {
  .grid-cols-1.md\:grid-cols-2.lg\:grid-cols-4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
