# Carrier IMS UI 与支持商业化改版设计

日期：2026-06-16

## 目标

重做主界面信息结构，减少单页堆叠，让核心 IMS 操作进入 App 后立即可用，同时加入附加网络工具、留言打赏、商业合作、广告配置、配置备份恢复和自动 APN 修改能力。

## 非目标

- 不把 TurboIMS 改成广告展示应用，广告和合作内容不能压过 IMS 主流程。
- 不在客户端保存 DoDoPay、Telegram 或广告后台密钥。
- 不在首页反复弹广告；弹窗必须受频率控制，并且可关闭。
- 不做完整广告后台本身；App 只消费后台配置。
- 不在 DoDoPay 内新增 TurboIMS 专用表、专用接口或专用后台。DoDoPay 是通用支付网关，不能被改成 TurboIMS 后台。
- 不承诺 APN 写入在所有系统和运营商组合上成功，失败必须给出明确结果。

## 平台与主任务

- 平台：Android 手机 App，Kotlin + Jetpack Compose + Material 3。
- 界面类型：工具型移动应用。
- 主任务：用户进入后能快速选择 SIM，查看 IMS 状态，调整核心 IMS/5G 功能。
- 视觉方向：Tooling，专业、紧凑、克制，优先层级和效率。

## 底栏结构

底栏固定 5 个页面：

- `IMS`：默认首页。保留 SIM 选择、Shizuku 状态、IMS 注册状态和核心 IMS/5G 开关。
- `附加`：放网络修复、抖音修复、网络出口检测、APN 与 SIM 信息、快捷开关、配置备份恢复、自动 APN 修改。
- `支持`：放留言支持作者、DoDoPay 打赏入口和打赏记录说明，不放广告。
- `合作`：放商务合作表单和合作广告内容。
- `关于`：放版本、更新、仓库、Issue、日志、Dump、免责声明。

## IMS 页面

IMS 页只服务核心通信配置：

- 顶部只显示必要的 Shizuku 状态和刷新入口；品牌、版本、更新、仓库等信息归到关于页，避免和关于页重复。
- SIM 选择保留在核心页，不能拆到独立页面。
- IMS 注册状态显示在功能开关前。
- 核心开关保留 VoLTE、VoWiFi、ViLTE、VoNR、Cross-SIM、UT、5G NR、5G 信号阈值、5G+ 图标、LTE 显示为 4G。
- 抖音修复从 IMS 页移到附加页。
- 更新、仓库、Issue、日志、打赏从 IMS 页移走。

## 附加页面

附加页是网络兼容工具箱，不做杂货铺。

### Wi-Fi 异常修复

- 保留短文案：`Wi‑Fi 异常修复`、`修复网络受限或感叹号问题`。
- 只显示一个状态型按钮：
  - 默认状态显示 `一键修复 Wi‑Fi 网络`。
  - 已修复状态显示 `恢复默认`。
  - 网络正常状态显示 `网络正常`，按钮不可点。
- 当前状态显示为 `当前：默认`、`当前：已修复`、`当前：读取失败`。

### 抖音修复

- 从 IMS 页移到附加页。
- 当前 SIM 是大陆 SIM 时显示开关。
- 非大陆 SIM 显示轻提示，不展示强操作。

### 网络出口检测

- 用户主动点击后才访问外部检测接口。
- 显示公网 IP、IPv4/IPv6、地区、运营商/ASN、代理/机房风险、常用服务连通性。
- 检测失败显示错误，不自动重试。

### APN 与 SIM 信息

- 显示运营商、MCC/MNC、ISO、ICCID 尾号、subId。
- 提供 `打开 APN 设置`。
- 自动 APN 修改放在同一区块的高级操作里。

### 自动 APN 修改

- 第一版提供用户确认后的 APN 写入能力。
- 写入前显示将要写入的名称、APN、类型、MCC/MNC。
- 使用 Shizuku instrumentation 走特权路径写入 Android APN Provider。
- 写入失败返回错误信息，不能静默失败。
- 默认模板只给当前 SIM 推导出的安全值；用户可进入系统 APN 设置继续编辑。

### 快捷开关

- 展示当前 App 已提供的 QS 图块：VoLTE 开关、IMS 状态。
- 提供添加说明，必要时跳转系统快捷设置。

### 配置备份/恢复

- 保存当前 SIM 配置快照到本地。
- 快照包含名称、创建时间、subId、SIM 标识、MCC/MNC、ISO、功能开关值。
- 恢复前校验当前 SIM 的 MCC/MNC 与备份是否匹配。
- 不匹配时要求二次确认。
- 第一版支持本地备份、恢复、删除；导入导出可后续扩展。

## 支持页面

支持页只承载赞助闭环。

### 留言支持作者

流程：

1. 用户填写留言。
2. 用户选择金额或输入自定义金额。
3. 用户点击 `支付宝支持` 或 `微信支持`。
4. App 内弹窗打开已配置的 DoDoPay 公开支持页，并把金额、昵称、留言、`channel` 和 `auto_checkout=1` 作为公开页面参数传入。
5. DoDoPay 自动创建公开支持订单，保存昵称、留言、金额、`client_ref` 和 `proof_key`。
6. DoDoPay 直接进入 `/pay/{order_id}`，只展示指定渠道的二维码和应付金额。
7. 用户支付成功后，DoDoPay 继续生成 `payment_proof`。
8. 支付记录和留言记录在 DoDoPay 中查看，TurboIMS App 不保存支付数据。

未配置 DoDoPay 公开支持页时，页面显示 `暂未开放打赏入口`，不静默失败。

### 打赏记录

- 记录归 DoDoPay 的通用付款记录能力。
- App 只消费 DoDoPay 的通用公开打赏 Feed，不查询 TurboIMS 专用记录接口。
- DoDoPay Feed 使用 `public_feed_id`，不能直接用 `app_id` 读取记录。
- App 展示字段只包含金额、时间、渠道、昵称和留言，不展示订单号、商户订单号、联系方式、尾差、回调信息或签名字段。
- 未配置 DoDoPay 公开 Feed 时，继续展示记录归属说明。

### 满额去广告

- TurboIMS 无账号系统，不新增登录或业务后端。
- App 首次运行生成本机 `client_ref`，随公开打赏页参数传给 DoDoPay。
- App 打开 DoDoPay 支持页时带 `client_ref` 和 `proof_key=support_unlock`。
- DoDoPay 只返回支付事实证明 `payment_proof`，不判断去广告，不保存 TurboIMS 权益状态。
- App 拦截 `/checkout/close#payment_proof=...`，兼容 `?payment_proof=...`。
- App 调用 `GET /api/public/payment-proofs/{payment_proof}` 验证支付事实。
- 只有 `valid=true`、`app_id` 等于当前 DoDoPay 支持页应用、`proof_key=support_unlock`、`client_ref` 等于本机值、`status=paid` 且单笔 `amount >= 100.00` 时，本机保存 `ad_free=true`。
- 去广告只保存在本机。更新 App 不丢；卸载重装、清除数据、换手机暂不恢复。

## 合作页面

- `商务合作` 提交到 Muggle Leads 的合作意向接口，不写入 DoDoPay。
- Muggle Leads 合作表单可用时，不显示 GitHub Issue 备用联系入口。
- 合作广告从 Muggle Leads 公开广告接口读取，作为合作页内容卡片，不抢占 IMS 首页和支持页。
- 旧广告位 `support/card` 只作为兼容读取，展示位置仍归到合作页；新广告位使用 `cooperation/card`。
- 首页广告弹窗由远端配置决定，受本地频率控制。

## 首页广告弹窗

- 支持远端配置。
- 弹窗只能低频出现。
- 本地记录每个广告的最后展示时间、关闭状态和版本。
- 用户关闭后，在配置指定周期内不再出现。
- 弹窗只有一个主动作和一个关闭动作。

## 远端接口边界

App 侧需要以下接口能力：

- 广告配置：由 Muggle Leads 返回合作卡片、首页弹窗、频率控制参数。公开广告字段使用 `tab`、`position`、`image_url`、`click_url`、`alt`、`title`、`width`、`max_height`、`fit`。
- 商务合作：由 Muggle Leads 接收称呼、联系方式、合作类型和合作需求。`intent_type` 只能使用 Muggle Leads 支持的 `ads`、`development`、`token_supply`、`other`。
- DoDoPay 打赏入口：App 内弹窗打开 DoDoPay 已有公开支持页 `/support/{app_id}`；金额、昵称、留言映射到 DoDoPay 通用 `payer_name`、`payer_contact`、`payer_message` 能力；用户在 App 内选择支付渠道后，App 附加 `channel=ALIPAY|WECHAT` 和 `auto_checkout=1` 请求 DoDoPay 直达支付。
- DoDoPay 返回 App：App 打开支持页时附加 `return_mode=close` 和 `return_label`；DoDoPay 支付页点击返回后跳转 `https://pay.dodododo.org/checkout/close`，App 只拦截这个固定关闭页并关闭支付弹窗。
- DoDoPay 支付页必须返回线上可访问的支付地址，不能返回 `localhost`、`127.0.0.1` 或 `[::1]` 这类开发地址；这类问题应在 DoDoPay 侧修正。
- DoDoPay 支付页当前只展示个人静态收款码，不要求也不暗示会从 WebView 拉起支付宝或微信 App。
- 打赏记录：App 可读取 DoDoPay 通用公开 Feed `/api/public/support-feeds/{public_feed_id}`；App 不调用 `/api/turboims/*` 这类专用接口，也不使用 `app_id` 读取记录。
- 去广告证明：App 通过 DoDoPay 公开验证接口 `/api/public/payment-proofs/{payment_proof}` 验证支付事实，再由 App 本地判断是否去广告。

所有远端接口 base URL 由 App 配置常量控制。未配置时，界面显示未配置状态。

构建期配置：

- `turboims.adApiBaseUrl` 或 `TURBOIMS_AD_API_BASE_URL`：广告配置服务地址；默认使用 `https://leads.3jiezhiwai.com`，App 只调用 `/api/sources/carrier-ims/ad-slots` 和兼容公开地址，不携带 API Key。
- `turboims.businessIntentBaseUrl` 或 `TURBOIMS_BUSINESS_INTENT_BASE_URL`：合作意向服务地址；默认使用 `https://leads.3jiezhiwai.com`。
- `turboims.dodopaySupportUrlTemplate` 或 `TURBOIMS_DODOPAY_SUPPORT_URL_TEMPLATE`：DoDoPay 公开支持页模板。生产默认使用 `https://pay.dodododo.org/support/app_c5b4614bad018dbd`。App 会附加 `amount`、`payer_name`、`payer_message`、`channel`、`auto_checkout`、`source`、`app_version`、`title`、`description`、`subject`、`button_text`、`return_mode`、`return_label` 查询参数。
- `turboims.dodopaySupportFeedUrl` 或 `TURBOIMS_DODOPAY_SUPPORT_FEED_URL`：DoDoPay 公开打赏 Feed 地址。未配置时不拉取记录，只显示 DoDoPay 保存说明。
- `turboims.businessContactText` 或 `TURBOIMS_BUSINESS_CONTACT_TEXT`：商务合作入口文案。
- `turboims.businessContactUrl` 或 `TURBOIMS_BUSINESS_CONTACT_URL`：没有表单能力时的备用跳转地址。

客户端不直接调用 DoDoPay 签名接口，因为 DoDoPay 的 `app_secret` 只能保存在服务端。

### 服务职责边界

- TurboIMS App：只负责展示、收集用户输入和在 App 内打开 DoDoPay 公开支持页，不保存任何服务端密钥。
- Muggle Leads：负责广告位、广告投放和商务合作意向；公开接口不需要 API Key，管理接口由 Muggle Leads 自己保护。
- DoDoPay：只负责通用支付网关能力，包括创建支付订单、通用付款留言字段、支付页、到账确认和签名回调。DoDoPay 内不得出现 TurboIMS 专用表名、路由名或后台页面。

DoDoPay 线上已支持通用付款留言字段：`payer_name`、`payer_contact`、`payer_message`。如果 DoDoPay 当前只有带签名的 `POST /api/v1/orders`，TurboIMS App 不能直接调用；正确下一步是在 DoDoPay 侧提供通用公开支持页，而不是新增 TurboIMS 独立支持服务。

### 落地顺序

1. 先落地 Muggle Leads：App 读取广告配置，并把商务合作表单提交到 `POST /api/sources/carrier-ims/intents`。
2. 打赏支付只配置 DoDoPay 公开支持页；没有公开页时保持未开放状态。
3. App 内打赏记录只消费 DoDoPay 通用公开 Feed，并在未配置或读取失败时保留简短说明。
4. 除非用户明确授权，否则 TurboIMS 侧不再修改 DoDoPay 代码、数据库或线上服务。

## 隐私与安全

- 网络出口检测必须由用户点击触发。
- 检测前文案说明会连接检测服务获取公网 IP。
- App 不保存支付密钥、TG Bot Token 或广告后台密钥。
- 广告后台管理 API Key 只用于开发或服务端管理广告位，不能写入客户端，也不能作为 App 请求头下发。
- 已登记广告位：`home/popup` 用于首页弹窗，`cooperation/card` 用于合作页内容卡片；旧 `support/card` 只做兼容。
- 打赏留言和付款记录由 DoDoPay 通用能力保存；商务合作意向由 Muggle Leads 保存。
- DoDoPay 只接收通用支付订单和回调，不保存 TurboIMS 专用业务数据。
- APN 写入必须二次确认，并在失败时给出错误。

## 验证方式

- 构建：`./gradlew :app:assembleDebug`。
- 单测：`./gradlew :app:testDebugUnitTest`，覆盖公开支持页配置、首页广告频控、备份恢复校验和 APN 草稿校验。
- 手动验证：
  - 首次打开默认进入 IMS 页。
  - 5 个底栏可切换。
  - IMS 页不再显示 Wi-Fi 修复、打赏、更新、仓库、Issue。
  - 附加页可以执行 Wi-Fi 修复/恢复、抖音开关、IP 检测、APN 设置跳转、配置备份恢复入口。
  - 支持页在未配置 DoDoPay 公开支持页时显示清晰状态。
  - 支持页不显示商务合作和广告内容。
  - 合作页显示商务合作表单，合作广告只在合作页展示。
  - 关于页保留更新、仓库、Issue、日志、Dump。
  - 首页广告弹窗默认每次启动都展示；广告接口显式返回 `interval_hours` 时，才按本地间隔频控。
  - 首页广告弹窗使用接近 App 页面宽度的轻量弹层，弱化边框，并显示一句开源维护说明：`开源项目要长期维护，这条广告会支持 Carrier IMS 继续更新。感谢理解。`

## Review 修复补充

- 广告服务和 DoDoPay 公开支持页不能写死为空，必须支持构建期配置；App 不保存 DoDoPay 密钥，不调用 DoDoPay 签名下单接口。
- App 不保存订单号，也不刷新支付状态；支付记录归 DoDoPay 通用付款记录。
- 首页广告关闭只在配置周期内不再展示，不能永久关闭同一广告。
- 配置备份恢复前必须比较当前 SIM 与备份的 MCC/MNC；不匹配时弹出二次确认。
- 配置备份必须保存并恢复国家/地区 MCC 覆盖输入，避免恢复后少一部分兼容配置。
- 自动 APN 修改必须在展示确认框前校验 MCC/MNC；写入时避免重复插入同一 subId/numeric/apn/type 的 APN，并给出明确失败原因。

## 设计纠偏记录

- 已回滚错误方向：不得在 DoDoPay 中新增 `TurboImsSupportOrder`、`/api/turboims/*` 等 TurboIMS 专用能力。
- 后续任何跨项目改动必须先更新被改项目自己的文档，明确通用目标、非目标、数据结构、接口、迁移和验证计划，再实现。
- 当前阶段只允许 TurboIMS App 接入 Muggle Leads 公开能力和 DoDoPay 通用公开能力；不新建独立 TurboIMS 支持服务，不能把其他项目改成 TurboIMS 后台。
