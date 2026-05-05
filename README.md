# AddNavigation

Paper 1.21.4 기반 A* 내비게이션 플러그인입니다.

플레이어에게 목적지까지의 경로를 계산하고, 플레이어 앞의 `ItemDisplay` 하나로 다음 진행 방향을 안내합니다.
복잡한 미로형 지형, 아머스탠드/NPC 목적지, 대규모 동시 접속 환경을 고려해 경로 계산과 렌더링이 틱 단위로 분산되도록 설계되어 있습니다.

## 주요 기능

- A* 기반 경로 탐색
- `ItemDisplay` 1개만 사용한 플레이어별 방향 표시
- XZ 평면 기준 방향 안내
- 목적지 주변 walkable block 자동 탐색
- 시작 지점 보정
- SQLite 기반 목적지 저장
- 저장된 포인트 메모리 캐시
- 대규모 서버용 세션 라운드로빈 처리
- 틱당 A* 계산 수 제한
- Java 플러그인 API 제공

## 지원 환경

- Paper `1.21.4`
- Java `21`
- Maven 프로젝트

## 명령어

| 명령어 | 설명 |
| --- | --- |
| `/navigation add <이름> <x> <y> <z>` | 현재 월드에 목적지 저장 |
| `/navigation remove <이름>` | 목적지 삭제 |
| `/navigation start <player> <이름>` | 대상 플레이어에게 내비게이션 시작 |
| `/navigation stop <player>` | 대상 플레이어 내비게이션 중지 |
| `/navigation reload` | 설정 리로드 |

Aliases:

- `/nav`
- `/길찾기`

권한:

```yaml
addnavigation.admin
```

## 성능 구조

AddNavigation은 많은 동접에서도 한 틱에 모든 작업이 몰리지 않도록 다음 구조를 사용합니다.

### 1. 세션 라운드로빈

모든 플레이어를 매 틱 모두 검사하지 않습니다.
`navigation.session-checks-per-tick` 수만큼만 검사하고, 다음 틱에 이어서 처리합니다.

예시:

```yaml
session-checks-per-tick: 250
```

ex) 동접 1000명이 모두 내비게이션 중이면 1틱마다 250명씩 전체 세션을 약 4틱마다 한 번씩 검사합니다.

### 2. A* 계산 큐

경로 계산은 즉시 전부 실행하지 않고 큐에 넣습니다.
한 틱에 실행할 최대 A* 계산 수는 `navigation.max-pathfinds-per-tick`으로 제한됩니다.

```yaml
max-pathfinds-per-tick: 3
```

이 값이 높으면 반응은 빨라지지만 순간 TPS 부하가 커집니다.

### 3. 렌더링 주기 제한

`ItemDisplay` 위치/방향 갱신도 매 틱 하지 않습니다.

```yaml
render-interval-ticks: 4
```

기본값 4틱은 플레이어당 초당 약 5회 업데이트입니다.

### 4. 포인트 메모리 캐시

목적지 조회와 탭완성은 매번 SQLite를 읽지 않습니다.
플러그인 로드/리로드 시 DB에서 포인트를 읽어 메모리에 캐시하고, 추가/삭제 시 캐시를 함께 갱신합니다.

### 5. 실패 경로 쿨다운

길을 찾지 못한 세션은 매 틱 다시 A*를 돌리지 않습니다.

```yaml
path-not-found-cooldown-ticks: 100
```

기본값은 5초입니다.

## 추천 설정

동접이 많은 서버 권장 기본값입니다.

```yaml
navigation:
  auto-stop-distance: 5.0
  session-checks-per-tick: 250
  max-pathfinds-per-tick: 3
  render-interval-ticks: 4
  repath-interval-ticks: 80
  repath-move-threshold: 3.0
  path-not-found-cooldown-ticks: 100
  max-search-nodes: 200000
  max-step-height: 1
  max-drop-height: 3
  start-search-radius: 2
  goal-search-radius: 6
  vertical-search-range: 4
```

더 큰 미로가 많으면:

- `max-search-nodes`를 `300000` 또는 `400000`으로 증가
- 목적지가 아머스탠드/NPC처럼 정확한 좌표가 막히면 `goal-search-radius`를 `8` 정도로 증가
- TPS가 흔들리면 `max-pathfinds-per-tick`을 낮추고 `repath-interval-ticks`를 높임

## ItemDisplay 설정

```yaml
display:
  item: ARROW
  custom-model-data: 0
  height-offset: 0.12
  distance: 2.25
  look-ahead-distance: 2.0
  scale: 0.85
  pitch-offset: 90.0
  yaw-offset: 0.0
  view-range: 16.0
  glowing: false
```

설정 설명:

| 키 | 설명 |
| --- | --- |
| `item` | `ItemDisplay`에 사용할 아이템 Material |
| `custom-model-data` | 리소스팩 커스텀 모델 사용 시 값 지정 |
| `height-offset` | 플레이어 발 위치 기준 표시 높이 |
| `distance` | 플레이어 앞 몇 블록에 표시할지 |
| `look-ahead-distance` | 다음 경로 지점을 고르는 최소 XZ 거리 |
| `scale` | 표시 크기 |
| `pitch-offset` | 아이템을 눕히는 X축 회전 |
| `yaw-offset` | 모델의 앞 방향 보정 |
| `view-range` | 클라이언트 표시 범위 |
| `glowing` | 발광 효과 여부 |

아이템이 서 있으면:

```yaml
pitch-offset: -90.0
```

화살표 방향이 90도 틀리면:

```yaml
yaw-offset: 90.0
```

또는 `180.0`, `-90.0`으로 맞추면 됩니다.

## 외부 플러그인에서 API 사용하기

### plugin.yml

AddNavigation이 반드시 먼저 로드되어야 한다면:

```yaml
depend:
  - AddNavigation
```

선택 연동이라면:

```yaml
softdepend:
  - AddNavigation
```

### API 가져오기
gradle
```gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
        implementation 'com.github.Antos1123:AddNavigation:1.0.0'
}
```

maven
```xml
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
    <groupId>com.github.Antos1123</groupId>
    <artifactId>AddNavigation</artifactId>
    <version>1.0.0</version>
</dependency>
```
`depend: [AddNavigation]`를 사용한다면 가장 간단하게 가져올 수 있습니다.

```java
import kr.antos112.addnavigation.AddNavigation;
import kr.antos112.addnavigation.api.AddNavigationAPI;

AddNavigation addNavigation = AddNavigation.getInstance();
AddNavigationAPI navigationApi = addNavigation.getNavigationAPI();
```

선택 연동이라면 Bukkit PluginManager로 안전하게 확인하세요.

```java
import kr.antos112.addnavigation.AddNavigation;
import kr.antos112.addnavigation.api.AddNavigationAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

Plugin plugin = Bukkit.getPluginManager().getPlugin("AddNavigation");
if (plugin instanceof AddNavigation addNavigation && plugin.isEnabled()) {
    AddNavigationAPI navigationApi = addNavigation.getNavigationAPI();
}
```

## API 메소드

```java
boolean savePoint(String name, Location location);
boolean savePoint(String name, String worldName, double x, double y, double z);
boolean removePoint(String name);
Optional<NavigationPoint> getPoint(String name);
Collection<NavigationPoint> getAllPoints();
boolean startNavigation(Player player, String pointName);
boolean stopNavigation(Player player);
Optional<NavigationSession> getSession(Player player);
void reload();
```

### 목적지 저장

```java
boolean saved = navigationApi.savePoint("spawn", player.getLocation());

if (!saved) {
    player.sendMessage("이미 같은 이름의 목적지가 있습니다.");
}
```

좌표로 저장:

```java
navigationApi.savePoint(
    "market",
    player.getWorld().getName(),
    100.5,
    64.0,
    -30.5
);
```

주의:

- 같은 이름이 이미 있으면 `false`를 반환합니다.
- 이름은 내부적으로 소문자 기준으로 저장됩니다.

### 목적지 삭제

```java
boolean removed = navigationApi.removePoint("market");
```

삭제된 목적지를 향해 가던 플레이어가 있으면 해당 세션도 자동 정리됩니다.

### 목적지 조회

```java
navigationApi.getPoint("spawn").ifPresent(point -> {
    String world = point.worldName();
    double x = point.x();
    double y = point.y();
    double z = point.z();
});
```

전체 목적지 조회:

```java
for (NavigationPoint point : navigationApi.getAllPoints()) {
    Bukkit.getLogger().info(point.name() + " -> " + point.worldName());
}
```

### 내비게이션 시작

```java
boolean started = navigationApi.startNavigation(player, "spawn");

if (!started) {
    player.sendMessage("목적지를 찾을 수 없거나 월드가 다릅니다.");
}
```

시작 조건:

- 목적지 이름이 존재해야 합니다.
- 플레이어 월드와 목적지 월드가 같아야 합니다.
- 기존 내비게이션이 있으면 자동으로 정리 후 새 세션이 시작됩니다.

### 내비게이션 중지

```java
boolean stopped = navigationApi.stopNavigation(player);
```

세션이 없으면 `false`를 반환합니다.

### 현재 세션 확인

```java
navigationApi.getSession(player).ifPresent(session -> {
    NavigationPoint target = session.getTarget();
    List<Location> currentPath = session.getCurrentPath();
});
```

`NavigationSession`은 런타임 상태입니다.
외부 플러그인에서 `ItemDisplay`를 직접 제거하거나 `currentPath`를 수정하지 않는 것을 권장합니다.

### 설정 리로드

```java
navigationApi.reload();
```

일반적으로 외부 플러그인에서 직접 호출하기보다 `/navigation reload` 사용을 권장합니다.

## 이벤트 연동 예시

플레이어가 특정 NPC를 클릭했을 때 내비게이션 시작:

```java
@EventHandler
public void onNpcClick(PlayerInteractEntityEvent event) {
    Player player = event.getPlayer();

    AddNavigation addNavigation = AddNavigation.getInstance();
    if (addNavigation == null || !addNavigation.isEnabled()) {
        return;
    }

    AddNavigationAPI api = addNavigation.getNavigationAPI();
    api.startNavigation(player, "quest_npc");
}
```

커스텀 메뉴에서 목적지 저장:

```java
public void registerWaypoint(Player admin, String name) {
    AddNavigationAPI api = AddNavigation.getInstance().getNavigationAPI();
    boolean saved = api.savePoint(name, admin.getLocation());

    admin.sendMessage(saved ? "저장 완료" : "이미 존재하는 목적지입니다.");
}
```

## 스레드 주의사항

API는 Bukkit 메인 스레드에서 호출하는 것을 기준으로 설계되어 있습니다.

특히 경로 탐색은 월드 블록 상태를 읽기 때문에 Bukkit/Paper 월드 API 접근이 필요합니다.
비동기 스레드에서 직접 호출하지 마세요.

비동기 작업 중 내비게이션을 시작해야 한다면:

```java
Bukkit.getScheduler().runTask(yourPlugin, () -> {
    AddNavigation.getInstance().getNavigationAPI().startNavigation(player, "spawn");
});
```

## 데이터 저장

기본 저장 위치:

```text
plugins/AddNavigation/navigation.db
```

config 파일:

```text
plugins/AddNavigation/config.yml
```

## 운영 팁

- 많은 인원이 동시에 내비게이션을 시작하는 상황은 큐로 분산됩니다.
- 즉시 반응성이 더 중요하면 `max-pathfinds-per-tick`을 조금 올리세요.
- TPS 안정성이 더 중요하면 `max-pathfinds-per-tick`을 낮추고 `repath-interval-ticks`를 높이세요.
- 복잡한 미로에서는 `max-search-nodes`와 `goal-search-radius`를 올리세요.
- 목적지가 아머스탠드/NPC면 정확한 좌표보다 주변 walkable block 탐색이 중요합니다.
- 리소스팩 사용 시 `custom-model-data`, `pitch-offset`, `yaw-offset`으로 모양을 맞추세요.
