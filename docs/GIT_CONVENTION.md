
 ## **Commit Type**

  | Type | Description |
  | --- | --- |
  | `feat` | 새로운 기능 추가 |
  | `fix` | 버그 수정 |
  | `style` | UI / CSS 수정 |
  | `refactor` | 코드 리팩토링 |
  | `docs` | 문서 수정 |
  | `test` | 테스트 코드 |
  | `chore` | 설정 및 패키지 관리 |

  ex) feat: 로그인 기능 구현

  ## **Branch**

    - 기능별로 브랜치 명을 만들고 issue로 관리
    - main과 develop 브랜치에는 직접 push하지 않습니다.

  | Branch | Description |
      | --- | --- |
  | main | 배포 브랜치 |
  | develop | 개발 통합 브랜치 |
  | feature/* | 기능 개발 브랜치 |
  | fix/* | 버그 수정 브랜치 |

  ex) fix/login-error
  </br></br>
  **Branch Naming**

    - 브랜치는 기능 또는 수정 단위로 생성하고, 관련 Issue 번호를 함께 작성
    - main과 develop 브랜치에는 직접 push하지 않습니다.

  | Type | Description | Example |
      | --- | --- | --- |
  | `feat/*` | 기능 개발 브랜치 | `feat/12-login` |
  | `fix/*` | 버그 수정 브랜치 | `fix/15-login-error` |
  | `style/*` | UI 및 CSS 수정 브랜치 | `style/20-main-page` |
  | `refactor/*` | 리팩토링 브랜치 | `refactor/25-reservation-service` |
  | `docs/*` | 문서 수정 브랜치 | `docs/30-readme` |
  | `test/*` | 테스트 코드 브랜치 | `test/35-reservation-test` |
  | `chore/*` | 설정 및 패키지 관리 브랜치 | `chore/40-project-setting` |

  - 브랜치명에 포함되는 번호는 PR 번호가 아닌 관련 Issue 번호를 기준으로 작성합니다.
  - PR 번호는 GitHub에서 Pull Request 생성 시 자동 부여됩니다.
</br>
## Branch 전략 ##

  main과 develop 브랜치에는 직접 push하지 않습니다.

  - 작업 흐름 Issue를 생성 이후 develop 브랜치에서 기능 브랜치 생성
  - 작업을 진행한 뒤 기능 브랜치에 commit과 push
  - 작업이 완료되면 Pull Request를 작성
  - 팀원 확인 후 develop 브랜치에 merge

  ## PR

  PR 작성 후 팀원 확인 뒤 merge 진행 / merge 먼저하기 절대 금지

  Title - `[FEAT] 로그인 기능 구현`

    ```jsx
    ## 작업 내용
    - 로그인 기능 구현
    
    ## 변경 사항
    - JWT 인증 처리 추가
    
    ## 테스트
    - [x] 로컬 테스트 완료
    
    ## 참고
    - 관련 Issue: #12
    ```
